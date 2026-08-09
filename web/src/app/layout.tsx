import { RootProvider } from 'fumadocs-ui/provider/next';
import './global.css';
import { Inter } from 'next/font/google';
import type { Metadata } from 'next';
import { appDescription, appName, getBaseUrl } from '@/lib/shared';

const inter = Inter({
  subsets: ['latin'],
});

/**
 * Site-wide metadata, and the base every page inherits.
 *
 * `metadataBase` is the important one: Open Graph images are declared as relative paths,
 * and without a base they resolve against localhost, which is why pasted links produced
 * no preview card.
 */
export const metadata: Metadata = {
  metadataBase: getBaseUrl(),
  title: {
    default: `${appName} — Display Entity NPCs for PaperMC`,
    // Page titles become "Getting Started — CocoNPC Documentation".
    template: `%s — ${appName} Documentation`,
  },
  description: appDescription,
  applicationName: appName,
  keywords: [
    'CocoNPC',
    'Minecraft',
    'PaperMC',
    'NPC plugin',
    'Display Entity',
    'Minecraft NPCs',
  ],
  openGraph: {
    type: 'website',
    siteName: `${appName} Documentation`,
    title: `${appName} — Display Entity NPCs for PaperMC`,
    description: appDescription,
    url: '/',
    images: [
      {
        url: '/og/image.png',
        width: 1200,
        height: 630,
        alt: `${appName} documentation`,
      },
    ],
  },
  twitter: {
    card: 'summary_large_image',
    title: `${appName} — Display Entity NPCs for PaperMC`,
    description: appDescription,
    images: ['/og/image.png'],
  },
};

export default function Layout({ children }: LayoutProps<'/'>) {
  return (
    <html lang="en" className={inter.className} suppressHydrationWarning>
      <body className="flex flex-col min-h-screen">
        <RootProvider>{children}</RootProvider>
      </body>
    </html>
  );
}
