// One-time script to fetch all 114 surahs (Arabic + English) and save as quran.json
// Run: node fetch_quran.js   (requires internet, run once to build the asset)

const https = require('https');
const fs = require('fs');
const path = require('path');

const OUTPUT_PATH = path.join(__dirname, 'app', 'src', 'main', 'assets', 'quran.json');

function get(url) {
  return new Promise((resolve, reject) => {
    https.get(url, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        try { resolve(JSON.parse(data)); }
        catch (e) { reject(new Error('Parse error: ' + e.message + '\nBody: ' + data.slice(0, 200))); }
      });
    }).on('error', reject);
  });
}

async function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }

async function main() {
  // Create assets dir if needed
  const assetsDir = path.dirname(OUTPUT_PATH);
  if (!fs.existsSync(assetsDir)) fs.mkdirSync(assetsDir, { recursive: true });

  const allSurahs = [];

  for (let surahNum = 1; surahNum <= 114; surahNum++) {
    const url = `https://api.alquran.cloud/v1/surah/${surahNum}/editions/ar.alafasy,en.sahih`;
    process.stdout.write(`Fetching surah ${surahNum}/114...\r`);

    let retries = 3;
    let resp = null;
    while (retries-- > 0) {
      try {
        resp = await get(url);
        break;
      } catch (e) {
        if (retries === 0) throw e;
        await sleep(2000);
      }
    }

    if (!resp || resp.code !== 200 || !resp.data || resp.data.length < 2) {
      throw new Error(`Bad response for surah ${surahNum}: ${JSON.stringify(resp).slice(0, 200)}`);
    }

    const arabicEdition = resp.data[0];
    const englishEdition = resp.data[1];

    const verses = arabicEdition.ayahs.map((ayah, i) => ({
      n: ayah.numberInSurah,
      a: ayah.text,
      e: (englishEdition.ayahs[i] || {}).text || ''
    }));

    allSurahs.push({ s: surahNum, v: verses });

    // Polite delay to avoid rate limiting
    await sleep(200);
  }

  fs.writeFileSync(OUTPUT_PATH, JSON.stringify(allSurahs), 'utf8');
  console.log(`\nDone! Saved ${allSurahs.length} surahs to:\n${OUTPUT_PATH}`);
}

main().catch(e => { console.error('\nError:', e.message); process.exit(1); });
