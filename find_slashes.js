const https = require('https');
const fs = require('fs');
const path = require('path');

const URL = "https://infoserver.hochschule-stralsund.de/daVinciIS.dll?content=json";
const LOCAL_FALLBACK = "/Users/heahl/Documents/HOST/mob_sys/Timetable/app/test.json";

function parseData(jsonString) {
    try {
        const root = JSON.parse(jsonString);
        const lessonTimes = root.result?.displaySchedule?.lessonTimes || [];
        
        const classesWithSlashes = new Set();
        const coursesWithSlashes = new Set();

        lessonTimes.forEach(lesson => {
            // Check classCodes (groups)
            if (Array.isArray(lesson.classCodes)) {
                lesson.classCodes.forEach(code => {
                    if (code.includes('/')) {
                        classesWithSlashes.add(code);
                    }
                });
            }

            // Check courseTitle
            if (lesson.courseTitle && lesson.courseTitle.includes('/')) {
                coursesWithSlashes.add(lesson.courseTitle);
            }
        });

        console.log("--- STUDIENGÄNGE / GRUPPEN MIT '/' IN IDENTIFIKATOREN ---");
        if (classesWithSlashes.size > 0) {
            Array.from(classesWithSlashes).sort().forEach(c => console.log(`  - Group: ${c}`));
        } else {
            console.log("  Keine Gruppen mit '/' gefunden.");
        }

        console.log("\n--- LEHRVERANSTALTUNGEN / KURSE MIT '/' IM TITEL ---");
        if (coursesWithSlashes.size > 0) {
            Array.from(coursesWithSlashes).sort().forEach(c => console.log(`  - Course: ${c}`));
        } else {
            console.log("  Keine Kurse mit '/' gefunden.");
        }
    } catch (e) {
        console.error("Fehler beim Parsen der JSON-Daten:", e.message);
    }
}

console.log("Fetsche Daten von: " + URL);
https.get(URL, (res) => {
    let data = '';
    res.on('data', (chunk) => { data += chunk; });
    res.on('end', () => {
        console.log("Fetch abgeschlossen. Verarbeite Daten...");
        parseData(data);
    });
}).on('error', (err) => {
    console.warn("Fetch fehlgeschlagen (" + err.message + "). Versuche lokale Fallback-Datei...");
    try {
        const localData = fs.readFileSync(LOCAL_FALLBACK, 'utf8');
        parseData(localData);
    } catch (e) {
        console.error("Lokales Fallback fehlgeschlagen:", e.message);
    }
});
