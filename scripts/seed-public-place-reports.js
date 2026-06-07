const firebaseAuth = require("C:/Users/khj010309/AppData/Roaming/npm/node_modules/firebase-tools/lib/auth.js");
const fs = require("fs");
const path = require("path");

const PROJECT_ID = "focustation-f7fe9";
const COLLECTION = "publicPlaceReports";
const GEO_CELL_DEGREES = 0.01;
const NAVER_LOCAL_SEARCH_ENDPOINT = "https://openapi.naver.com/v1/search/local.json";

const SEARCHES = [
  {query: "동작구 스터디카페", limit: 6},
  {query: "동작구 조용한 카페", limit: 5},
  {query: "상도동 스터디카페", limit: 5},
  {query: "노량진 스터디카페", limit: 5},
  {query: "관악구 스터디카페", limit: 6},
  {query: "서울대입구 스터디카페", limit: 6},
  {query: "신림 스터디카페", limit: 6},
  {query: "관악구 조용한 카페", limit: 5},
  {query: "금천구 스터디카페", limit: 6},
  {query: "가산디지털단지역 카페", limit: 5},
  {query: "독산동 스터디카페", limit: 5},
  {query: "시흥동 스터디카페", limit: 5},
];

function geoCellKey(latitude, longitude) {
  const latIndex = Math.floor(latitude / GEO_CELL_DEGREES);
  const lngIndex = Math.floor(longitude / GEO_CELL_DEGREES);
  return `${latIndex}:${lngIndex}`;
}

function docId(name, latitude, longitude) {
  const normalized = name
    .trim()
    .toLowerCase()
    .replace(/[/#?[\]]/g, "_")
    .replace(/\s+/g, "_")
    .slice(0, 80);
  return `seed_${geoCellKey(latitude, longitude)}_${normalized}`;
}

function firestoreValue(value) {
  if (value === null || value === undefined) {
    return {nullValue: null};
  }
  if (typeof value === "string") {
    return {stringValue: value};
  }
  if (typeof value === "number") {
    return Number.isInteger(value) ?
      {integerValue: String(value)} :
      {doubleValue: value};
  }
  if (typeof value === "boolean") {
    return {booleanValue: value};
  }
  if (value instanceof Date) {
    return {timestampValue: value.toISOString()};
  }
  if (typeof value === "object") {
    return {
      mapValue: {
        fields: Object.fromEntries(
          Object.entries(value).map(([key, nested]) => [
            key,
            firestoreValue(nested),
          ]),
        ),
      },
    };
  }
  throw new Error(`Unsupported Firestore value: ${value}`);
}

function firestoreFields(record) {
  return Object.fromEntries(
    Object.entries(record).map(([key, value]) => [key, firestoreValue(value)]),
  );
}

function cleanNaverText(value) {
  return String(value || "")
    .replace(/<[^>]*>/g, "")
    .replace(/&amp;/g, "&")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&quot;/g, "\"")
    .trim();
}

function readLocalProperties() {
  const filePath = path.resolve(__dirname, "..", "local.properties");
  const text = fs.readFileSync(filePath, "utf8");
  return Object.fromEntries(
    text
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter((line) => line && !line.startsWith("#") && line.includes("="))
      .map((line) => {
        const index = line.indexOf("=");
        return [line.slice(0, index).trim(), line.slice(index + 1).trim()];
      }),
  );
}

function naverCredentials() {
  const localProperties = readLocalProperties();
  return {
    clientId:
      localProperties.NAVER_SEARCH_CLIENT_ID ||
      localProperties.NAVER_SEARCH_CLIENTID ||
      localProperties.NAVER_CLIENT_ID,
    clientSecret:
      localProperties.NAVER_SEARCH_CLIENT_SECRET ||
      localProperties.NAVER_SEARCH_CLIENTSECRET ||
      localProperties.NAVER_CLIENT_SECRET,
  };
}

function toLatLng(item) {
  const longitude = Number(item.mapx) / 1e7;
  const latitude = Number(item.mapy) / 1e7;
  if (
    !Number.isFinite(latitude) ||
    !Number.isFinite(longitude) ||
    latitude < 33 ||
    latitude > 39 ||
    longitude < 124 ||
    longitude > 132
  ) {
    return null;
  }
  return {latitude, longitude};
}

async function searchNaverPlaces(query, limit, credentials) {
  const url = new URL(NAVER_LOCAL_SEARCH_ENDPOINT);
  url.searchParams.set("query", query);
  url.searchParams.set("display", String(Math.min(limit, 10)));
  url.searchParams.set("start", "1");
  url.searchParams.set("sort", "comment");

  const response = await fetch(url, {
    headers: {
      Accept: "application/json",
      "X-Naver-Client-Id": credentials.clientId,
      "X-Naver-Client-Secret": credentials.clientSecret,
    },
  });

  const text = await response.text();
  if (!response.ok) {
    throw new Error(`Naver search failed for ${query}: ${response.status} ${text}`);
  }

  const items = JSON.parse(text).items || [];
  return items.map((item) => {
    const latLng = toLatLng(item);
    if (!latLng) return null;
    return {
      placeName: cleanNaverText(item.title),
      category: cleanNaverText(item.category),
      address: cleanNaverText(item.address),
      roadAddress: cleanNaverText(item.roadAddress),
      latitude: latLng.latitude,
      longitude: latLng.longitude,
    };
  }).filter(Boolean);
}

function seededMetrics(index) {
  const noiseValues = [34.8, 38.6, 41.2, 45.7, 49.5, 53.1, 57.8];
  const lightValues = [310.4, 380.2, 455.7, 520.8, 610.5, 720.3];
  const vibrationValues = [0.014, 0.019, 0.024, 0.031, 0.038, 0.047];
  const avgNoise = noiseValues[index % noiseValues.length];
  const avgIlluminance = lightValues[(index * 2) % lightValues.length];
  const avgVibration = vibrationValues[(index * 3) % vibrationValues.length];
  const mlScoreAvg = Math.max(
    62,
    Math.min(94, 94 - Math.max(0, avgNoise - 36) * 0.7 - avgVibration * 120),
  );

  return {
    reportCount: 3 + (index % 15),
    mlScoreAvg: Math.round(mlScoreAvg * 10) / 10,
    avgNoise,
    avgIlluminance,
    avgVibration,
    environmentSummary: {
      noise: avgNoise < 40 ? "low" : avgNoise < 70 ? "moderate" : "high",
      light: avgIlluminance <= 0 ?
        "unknown" :
        avgIlluminance < 100 ?
          "too_dark" :
          avgIlluminance < 1500 ?
            "comfortable" :
            "too_bright",
      vibration: avgVibration < 0.03 ? "low" : avgVibration < 0.1 ? "moderate" : "high",
    },
  };
}

async function buildPlaces() {
  const credentials = naverCredentials();
  if (!credentials.clientId || !credentials.clientSecret) {
    throw new Error("Naver local search credentials are missing.");
  }

  const seen = new Set();
  const places = [];
  for (const search of SEARCHES) {
    const results = await searchNaverPlaces(search.query, search.limit, credentials);
    for (const result of results) {
      const key = [
        result.placeName,
        result.roadAddress,
        result.address,
        result.latitude,
        result.longitude,
      ].join("|");
      if (seen.has(key)) continue;
      seen.add(key);
      places.push({
        ...result,
        ...seededMetrics(places.length),
      });
    }
  }

  return places;
}

async function getFirebaseAccessToken() {
  const account = firebaseAuth.getGlobalDefaultAccount();
  const refreshToken = account?.tokens?.refresh_token;
  if (!refreshToken) {
    throw new Error("Firebase CLI login token was not found.");
  }

  const tokens = await firebaseAuth.getAccessToken(refreshToken, []);
  if (!tokens?.access_token) {
    throw new Error("Could not get Firebase access token.");
  }
  return tokens.access_token;
}

async function main() {
  const accessToken = await getFirebaseAccessToken();
  const now = new Date();
  const places = await buildPlaces();
  const writes = places.map((place) => {
    const placeKey = docId(place.placeName, place.latitude, place.longitude);
    const record = {
      ...place,
      placeKey,
      geoCellKey: geoCellKey(place.latitude, place.longitude),
      lastSessionId: `seed-${placeKey}`,
      lastReportedAt: now,
      createdAt: now,
      updatedAt: now,
      isSeed: true,
    };

    return {
      update: {
        name: `projects/${PROJECT_ID}/databases/(default)/documents/${COLLECTION}/${placeKey}`,
        fields: firestoreFields(record),
      },
    };
  });

  const response = await fetch(
    `https://firestore.googleapis.com/v1/projects/${PROJECT_ID}/databases/(default)/documents:batchWrite`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({writes}),
    },
  );
  const body = await response.text();

  if (!response.ok) {
    throw new Error(`Firestore seed failed: ${response.status} ${body}`);
  }

  console.log(`Seeded ${places.length} public place reports.`);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
