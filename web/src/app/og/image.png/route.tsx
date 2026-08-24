import { ImageResponse } from 'next/og';
import { generate as DefaultImage } from 'fumadocs-ui/og';
import { appName } from '@/lib/shared';

/**
 * The link preview image for the site root.
 *
 * Docs pages each get their own generated card under /og/docs; this is the one used when
 * the bare domain is pasted, so a shared link never falls back to no image at all.
 */
export const revalidate = false;

export function GET() {
  return new ImageResponse(
    (
      <DefaultImage
        title={appName}
        description="Display Entity NPCs for PaperMC — per-limb posing, joint bending and persistent click actions."
        site="Documentation"
      />
    ),
    {
      width: 1200,
      height: 630,
    },
  );
}
