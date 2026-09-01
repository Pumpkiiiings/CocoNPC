import { createOpenRouter } from '@openrouter/ai-sdk-provider';
import {
  convertToModelMessages,
  createUIMessageStreamResponse,
  stepCountIs,
  streamText,
  tool,
  toUIMessageStream,
} from 'ai';
import { z } from 'zod';
import { source } from '@/lib/source';
import { Document, type DocumentData } from 'flexsearch';
import { ChatUIMessage, SearchTool } from '../../../components/ai/search';
import { consumeRateLimit } from '@/lib/rate-limit';
import {
  isSameOriginRequest,
  requestBodyLimit,
  requestClientKey,
} from '@/lib/request-security';

export const maxDuration = 30;

const messageSchema = z.object({
  id: z.string().max(128).optional(),
  role: z.enum(['user', 'assistant', 'system']),
  parts: z.array(z.unknown()).max(50),
}).passthrough();

const requestSchema = z.object({
  messages: z.array(messageSchema).min(1).max(20),
}).strict();

function jsonError(message: string, status: number, headers?: HeadersInit) {
  return Response.json({ error: message }, { status, headers });
}

interface CustomDocument extends DocumentData {
  url: string;
  title: string;
  description: string;
  content: string;
}
const searchServer = createSearchServer();

async function createSearchServer() {
  const search = new Document<CustomDocument>({
    document: {
      id: 'url',
      index: ['title', 'description', 'content'],
      store: true,
    },
  });

  const docs = await chunkedAll(
    source.getPages().map(async (page) => {
      if (!('getText' in page.data)) return null;

      return {
        title: page.data.title,
        description: page.data.description,
        url: page.url,
        content: await page.data.getText('processed'),
      } as CustomDocument;
    }),
  );

  for (const doc of docs) {
    if (doc) search.add(doc);
  }

  return search;
}

async function chunkedAll<O>(promises: Promise<O>[]): Promise<O[]> {
  const SIZE = 50;
  const out: O[] = [];
  for (let i = 0; i < promises.length; i += SIZE) {
    out.push(...(await Promise.all(promises.slice(i, i + SIZE))));
  }
  return out;
}

const openrouter = createOpenRouter({
  apiKey: process.env.OPENROUTER_API_KEY,
});

/** System prompt, you can update it to provide more specific information */
const systemPrompt = [
  'You are an AI assistant for a documentation site.',
  'Use the `search` tool to retrieve relevant docs context before answering when needed.',
  'The `search` tool returns raw JSON results from documentation. Use those results to ground your answer and cite sources as markdown links using the document `url` field when available.',
  'If you cannot find the answer in search results, say you do not know and suggest a better search query.',
].join('\n');

export async function POST(req: Request) {
  if (!process.env.OPENROUTER_API_KEY) {
    return jsonError('Chat is not configured.', 503);
  }
  if (req.headers.get('content-type')?.split(';')[0] !== 'application/json') {
    return jsonError('Content-Type must be application/json.', 415);
  }
  if (!isSameOriginRequest(req)) {
    return jsonError('Origin is not allowed.', 403);
  }

  const limit = consumeRateLimit(requestClientKey(req), {
    limit: 10,
    windowMs: 60_000,
  });
  const rateHeaders = {
    'RateLimit-Limit': String(limit.limit),
    'RateLimit-Remaining': String(limit.remaining),
    'RateLimit-Reset': String(Math.ceil(limit.resetAt / 1000)),
  };
  if (!limit.allowed) {
    return jsonError('Too many requests.', 429, {
      ...rateHeaders,
      'Retry-After': String(Math.max(1, Math.ceil((limit.resetAt - Date.now()) / 1000))),
    });
  }

  const bodyLimit = requestBodyLimit();
  const declaredLength = Number.parseInt(req.headers.get('content-length') ?? '0', 10);
  if (declaredLength > bodyLimit) {
    return jsonError('Request body is too large.', 413, rateHeaders);
  }

  const rawBody = await req.text();
  if (new TextEncoder().encode(rawBody).byteLength > bodyLimit) {
    return jsonError('Request body is too large.', 413, rateHeaders);
  }

  let reqJson: z.infer<typeof requestSchema>;
  try {
    reqJson = requestSchema.parse(JSON.parse(rawBody));
  } catch {
    return jsonError('Invalid chat request.', 400, rateHeaders);
  }

  const result = streamText({
    model: openrouter.chat(process.env.OPENROUTER_MODEL ?? 'anthropic/claude-3.5-sonnet'),
    stopWhen: stepCountIs(5),
    tools: {
      search: searchTool,
    },
    messages: [
      { role: 'system', content: systemPrompt },
      ...(await convertToModelMessages<ChatUIMessage>(
        reqJson.messages as Omit<ChatUIMessage, 'id'>[],
        {
          convertDataPart(part) {
            if (part.type === 'data-client')
              return {
                type: 'text',
                text: `[Client Context: ${JSON.stringify(part.data)}]`,
              };
          },
        },
      )),
    ],
    toolChoice: 'auto',
  });

  const response = createUIMessageStreamResponse({
    stream: toUIMessageStream({ stream: result.stream }),
  });
  for (const [name, value] of Object.entries(rateHeaders)) {
    response.headers.set(name, value);
  }
  return response;
}

const searchTool = tool({
  description: 'Search the docs content and return raw JSON results.',
  inputSchema: z.object({
    query: z.string().trim().min(1).max(200),
    limit: z.number().int().min(1).max(10).default(5),
  }),
  async execute({ query, limit }) {
    const search = await searchServer;
    return await search.searchAsync(query, { limit, merge: true, enrich: true });
  },
}) satisfies SearchTool;
