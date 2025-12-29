import puppeteer from 'puppeteer';
import path from 'path';
import { fileURLToPath } from 'url';
import fs from 'fs';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const SCREENSHOT_DIR = path.join(__dirname, 'screenshots');

// Ensure screenshot directory exists
if (!fs.existsSync(SCREENSHOT_DIR)) {
    fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });
}

console.log('Taking screenshot of running Matrix Background...');

const browser = await puppeteer.launch({
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox']
});

try {
    const page = await browser.newPage();
    await page.setViewport({ width: 1024, height: 768 });

    const port = process.argv[2] || '8080';
    console.log(`Navigating to http://localhost:${port}...`);
    await page.goto(`http://localhost:${port}`, {
        waitUntil: 'networkidle0',
        timeout: 60000
    });

    console.log('Waiting for animation to populate...');
    await new Promise(resolve => setTimeout(resolve, 12000));

    const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
    const screenshotPath = path.join(SCREENSHOT_DIR, `matrix_${timestamp}.png`);
    await page.screenshot({ path: screenshotPath, fullPage: true });
    console.log(`Screenshot saved: ${screenshotPath}`);

    // Take a second screenshot after more animation
    await new Promise(resolve => setTimeout(resolve, 3000));
    const screenshot2Path = path.join(SCREENSHOT_DIR, `matrix_${timestamp}_2.png`);
    await page.screenshot({ path: screenshot2Path, fullPage: true });
    console.log(`Screenshot saved: ${screenshot2Path}`);

} finally {
    await browser.close();
    console.log('Done.');
}
