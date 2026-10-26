import { defineConfig, loadEnv } from 'vite'
import path from 'path'
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'


export default defineConfig(({ mode }) => {
  const environment = loadEnv(mode, process.cwd(), 'VITE_');
  const devEnvironment = loadEnv(mode, process.cwd(), 'LADUX_DEV_');
  const publicDevHost = devEnvironment.LADUX_DEV_PUBLIC_URL
    ? new URL(devEnvironment.LADUX_DEV_PUBLIC_URL).hostname : undefined;
  const indexable = environment.VITE_ALLOW_INDEXING === 'true';
  const siteUrl = environment.VITE_SITE_URL?.replace(/\/$/, '');
  if (indexable && (!siteUrl || !/^https:\/\/[^/]+$/.test(siteUrl))) {
    throw new Error('An indexable production build requires VITE_SITE_URL with an HTTPS origin');
  }
  return {
  plugins: [
    react(),
    tailwindcss(),
    {
      name: 'ladux-public-indexing',
      transformIndexHtml(html) {
        return html.replace('content="noindex, nofollow"', `content="${indexable ? 'index, follow' : 'noindex, nofollow'}"`);
      },
      generateBundle() {
        const privatePaths = ['/admin', '/account', '/cart', '/checkout', '/orders', '/login', '/register', '/payment'];
        this.emitFile({ type: 'asset', fileName: 'robots.txt', source: indexable
          ? `User-agent: *\nAllow: /\n${privatePaths.map(value => `Disallow: ${value}`).join('\n')}\nSitemap: ${siteUrl}/sitemap.xml\n`
          : 'User-agent: *\nDisallow: /\n' });
        if (indexable) this.emitFile({ type: 'asset', fileName: 'sitemap.xml', source:
          '<?xml version="1.0" encoding="UTF-8"?><urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">'
          + ['/', '/products', '/smartfit', '/about', '/contact'].map(value => `<url><loc>${siteUrl}${value}</loc></url>`).join('')
          + '</urlset>' });
      },
    },
  ],
  resolve: {
    alias: {
      // Alias @ to the src directory
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    allowedHosts: publicDevHost ? [publicDevHost] : [],
    proxy: {
      '/api': { target: 'http://127.0.0.1:8080', changeOrigin: true },
      '/uploads': { target: 'http://127.0.0.1:8080', changeOrigin: true },
      '/oauth2': { target: 'http://127.0.0.1:8080', changeOrigin: true },
      '/login/oauth2': { target: 'http://127.0.0.1:8080', changeOrigin: true },
    },
  },

  // File types to support raw imports. Never add .css, .tsx, or .ts files to this.
  assetsInclude: ['**/*.svg', '**/*.csv'],
  };
})
