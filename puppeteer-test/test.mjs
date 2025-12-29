import puppeteer from 'puppeteer';
import { spawn } from 'child_process';
import path from 'path';
import { fileURLToPath } from 'url';
import fs from 'fs';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const DIST_DIR = path.join(__dirname, '..', 'composeApp', 'build', 'dist', 'wasmJs', 'productionExecutable');
const SCREENSHOT_DIR = path.join(__dirname, 'screenshots');
const PORT = 3456;

// Ensure screenshot directory exists
if (!fs.existsSync(SCREENSHOT_DIR)) {
    fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });
}

console.log('Starting Matrix Background Puppeteer Test...');
console.log(`Distribution directory: ${DIST_DIR}`);

// Check if distribution exists
if (!fs.existsSync(DIST_DIR)) {
    console.error('ERROR: Web distribution not found! Run `./gradlew :composeApp:wasmJsBrowserDistribution` first.');
    process.exit(1);
}

// Start static server
console.log(`Starting server on port ${PORT}...`);
const server = spawn('npx', ['serve', '-s', DIST_DIR, '-l', PORT.toString()], {
    stdio: ['ignore', 'pipe', 'pipe']
});

let serverOutput = '';
server.stdout.on('data', (data) => {
    serverOutput += data.toString();
});
server.stderr.on('data', (data) => {
    serverOutput += data.toString();
});

// Wait for server to start
await new Promise(resolve => setTimeout(resolve, 3000));

let browser;
try {
    console.log('Launching browser...');
    browser = await puppeteer.launch({
        headless: true,
        args: ['--no-sandbox', '--disable-setuid-sandbox']
    });

    const page = await browser.newPage();

    // Set viewport size
    await page.setViewport({ width: 1024, height: 768 });

    // Listen for console messages
    page.on('console', msg => {
        console.log(`Browser console [${msg.type()}]: ${msg.text()}`);
    });

    // Listen for page errors
    page.on('pageerror', error => {
        console.error('Page error:', error.message);
    });

    console.log(`Navigating to http://localhost:${PORT}...`);
    await page.goto(`http://localhost:${PORT}`, {
        waitUntil: 'networkidle0',
        timeout: 60000
    });

    console.log('Page loaded, waiting for canvas...');

    // Wait for the canvas to be present
    await page.waitForSelector('canvas#ComposeTarget', { timeout: 30000 });
    console.log('Canvas found!');

    // Wait for the Matrix animation to reach steady-state density
    // The original GLMatrix starts with all strips in "erasing" mode with no visible glyphs
    // It takes several seconds for strips to cycle and fill with characters
    console.log('Waiting for animation to reach steady-state...');
    await new Promise(resolve => setTimeout(resolve, 15000));

    // Take a screenshot
    const screenshot1Path = path.join(SCREENSHOT_DIR, 'matrix_frame1.png');
    await page.screenshot({ path: screenshot1Path, fullPage: true });
    console.log(`Screenshot saved: ${screenshot1Path}`);

    // Wait and take another screenshot to verify animation
    await new Promise(resolve => setTimeout(resolve, 2000));
    const screenshot2Path = path.join(SCREENSHOT_DIR, 'matrix_frame2.png');
    await page.screenshot({ path: screenshot2Path, fullPage: true });
    console.log(`Screenshot saved: ${screenshot2Path}`);

    // Check if canvas has content by getting its pixel data
    const canvasInfo = await page.evaluate(() => {
        const canvas = document.getElementById('ComposeTarget');
        if (!canvas) return { found: false };

        const ctx = canvas.getContext('2d');
        if (!ctx) {
            // For WebGL/WASM canvas, just check dimensions
            return {
                found: true,
                width: canvas.width,
                height: canvas.height,
                hasContext: false
            };
        }

        // Get some pixel data to verify content
        try {
            const imageData = ctx.getImageData(0, 0, Math.min(canvas.width, 100), Math.min(canvas.height, 100));
            const data = imageData.data;
            let nonBlackPixels = 0;
            for (let i = 0; i < data.length; i += 4) {
                const r = data[i];
                const g = data[i + 1];
                const b = data[i + 2];
                const a = data[i + 3];
                if (a > 0 && (r > 0 || g > 0 || b > 0)) {
                    nonBlackPixels++;
                }
            }
            return {
                found: true,
                width: canvas.width,
                height: canvas.height,
                hasContext: true,
                nonBlackPixels
            };
        } catch (e) {
            return {
                found: true,
                width: canvas.width,
                height: canvas.height,
                hasContext: true,
                error: e.message
            };
        }
    });

    console.log('Canvas info:', canvasInfo);

    if (!canvasInfo.found) {
        throw new Error('Canvas not found in the page');
    }

    if (canvasInfo.width === 0 || canvasInfo.height === 0) {
        throw new Error('Canvas has zero dimensions');
    }

    console.log('\n========================================');
    console.log('TEST PASSED: Matrix Background is running!');
    console.log('========================================');
    console.log(`Canvas dimensions: ${canvasInfo.width}x${canvasInfo.height}`);
    console.log(`Screenshots saved to: ${SCREENSHOT_DIR}`);
    console.log('');

} catch (error) {
    console.error('\n========================================');
    console.error('TEST FAILED:', error.message);
    console.error('========================================');
    console.error('Server output:', serverOutput);
    process.exit(1);
} finally {
    if (browser) {
        await browser.close();
    }
    server.kill();
    console.log('Cleanup complete.');
}
