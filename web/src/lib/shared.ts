export const appName = 'CocoNPC';

export const appDescription =
  'Lightweight, fully packet-driven Minecraft NPCs built from Display Entities. ' +
  'Per-limb posing, joint bending and persistent click actions, with no entity tick cost.';

export const docsRoute = '/docs';
export const docsImageRoute = '/og/docs';
export const docsContentRoute = '/llms.mdx/docs';

export const supportDiscord = 'https://discord.gg/ydsUw5UJrB';

/** Where the docs are deployed. Used to build absolute link-preview URLs. */
export const siteUrl = 'https://coconpc.vercel.app';

export const gitConfig = {
  user: 'Pumpkiiiings',
  repo: 'CocoNPC',
  branch: 'main',
};

/**
 * Absolute base URL of the deployed site.
 *
 * <p>Next needs this to turn the relative Open Graph image paths into absolute URLs.
 * Without it they resolve against localhost, so link previews in Discord, Twitter and
 * search results silently come out blank.
 *
 * <p>Defaults to the production deployment, so previews work with no configuration.
 * `NEXT_PUBLIC_SITE_URL` overrides it if the site ever moves to its own domain, and
 * Vercel branch deploys preview themselves rather than production.
 */
export function getBaseUrl(): URL {
  if (process.env.NEXT_PUBLIC_SITE_URL) {
    return new URL(process.env.NEXT_PUBLIC_SITE_URL);
  }
  if (process.env.VERCEL_ENV === 'preview' && process.env.VERCEL_URL) {
    return new URL(`https://${process.env.VERCEL_URL}`);
  }
  if (process.env.NODE_ENV === 'development') {
    return new URL(`http://localhost:${process.env.PORT ?? 3000}`);
  }
  return new URL(siteUrl);
}
