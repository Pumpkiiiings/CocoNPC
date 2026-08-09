# .

This is a Next.js application generated with
[Create Fumadocs](https://github.com/fuma-nama/fumadocs).

Run development server:

```bash
npm run dev
# or
pnpm dev
# or
yarn dev
```

Open http://localhost:3000 with your browser to see the result.

## Explore

In the project, you can see:

- `lib/source.ts`: Code for content source adapter, [`loader()`](https://fumadocs.dev/docs/headless/source-api) provides the interface to access your content.
- `lib/layout.shared.tsx`: Shared options for layouts, optional but preferred to keep.

| Route                     | Description                                            |
| ------------------------- | ------------------------------------------------------ |
| `app/(home)`              | The route group for your landing page and other pages. |
| `app/docs`                | The documentation layout and pages.                    |
| `app/api/search/route.ts` | The Route Handler for search.                          |

### Fumadocs MDX

A `source.config.ts` config file has been included, you can customise different options like frontmatter schema.

Read the [Introduction](https://fumadocs.dev/docs/mdx) for further details.

## Learn More

To learn more about Next.js and Fumadocs, take a look at the following
resources:

- [Next.js Documentation](https://nextjs.org/docs) - learn about Next.js
  features and API.
- [Learn Next.js](https://nextjs.org/learn) - an interactive Next.js tutorial.
- [Fumadocs](https://fumadocs.dev) - learn about Fumadocs

## Link previews (Open Graph)

Pasting a link to this site in Discord, Twitter or Slack produces a preview card. Each
docs page generates its own image under `/og/docs/<slug>/image.png`; the site root uses
`/og/image.png`.

For those images to resolve, Next needs the site's absolute URL. It defaults to the
production deployment, <https://coconpc.vercel.app>, so no configuration is needed.

Override it only if the site moves to its own domain:

```bash
NEXT_PUBLIC_SITE_URL=https://your-domain.example
```

Vercel preview deploys point at themselves rather than production, and local development
falls back to `http://localhost:3000`.

If a card ever shows stale text, the platform has cached it — Discord and Twitter both
cache aggressively. Use their respective debuggers to refresh.
