"""
Upload scraper output (places.json + events.json) directly to Firestore.

Why this exists: the Compose Desktop app can't initialize Firebase (the gitlive Firebase
library expects an Android Context even on JVM), so the in-app SYNC button is a no-op
on Desktop. This script bypasses the running app and writes to Firestore using the
Firebase Admin SDK with a service-account key.

Setup (one-time):
  1. Firebase Console → Project Settings → Service Accounts → "Generate new private key".
     The default project is `gusur-57e95`.
     https://console.firebase.google.com/project/gusur-57e95/settings/serviceaccounts/adminsdk
  2. Save the downloaded JSON as scraper/service-account.json (gitignored).
  3. pip install -r requirements.txt

Usage:
  python upload.py                          # uploads places.json + events.json
  python upload.py --dry-run                # parse + report counts, no writes
  python upload.py --collection places      # upload one collection
  python upload.py --key ~/keys/sa.json     # custom service account path
"""
import argparse
import json
import sys
from pathlib import Path

import firebase_admin
from firebase_admin import credentials, firestore

DEFAULT_KEY = Path(__file__).parent / "service-account.json"
SCRIPT_DIR = Path(__file__).parent
FIRESTORE_BATCH_LIMIT = 400  # Firestore caps at 500 per batch; leave headroom.


def parse_args():
    parser = argparse.ArgumentParser(description="Upload Gusur scraper output to Firestore.")
    parser.add_argument(
        "--key",
        type=Path,
        default=DEFAULT_KEY,
        help=f"Path to service-account JSON key (default: {DEFAULT_KEY}).",
    )
    parser.add_argument(
        "--collection",
        choices=("places", "events", "both"),
        default="both",
        help="Which collection(s) to upload (default: both).",
    )
    parser.add_argument(
        "--places",
        type=Path,
        default=SCRIPT_DIR / "places.json",
        help="Path to places.json.",
    )
    parser.add_argument(
        "--events",
        type=Path,
        default=SCRIPT_DIR / "events.json",
        help="Path to events.json.",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Parse the JSON files and report what would be uploaded, but don't write.",
    )
    return parser.parse_args()


def upload_collection(db, name: str, docs: list, id_field: str = "id") -> None:
    if not docs:
        print(f"  {name}: nothing to upload.")
        return
    written = 0
    for start in range(0, len(docs), FIRESTORE_BATCH_LIMIT):
        chunk = docs[start : start + FIRESTORE_BATCH_LIMIT]
        batch = db.batch()
        for doc in chunk:
            doc_id = doc.get(id_field)
            if not doc_id:
                print(f"  {name}: skipping doc without {id_field!r}: {doc!r}")
                continue
            ref = db.collection(name).document(str(doc_id))
            batch.set(ref, doc)
        batch.commit()
        written += len(chunk)
        print(f"  {name}: wrote {written}/{len(docs)}")


def load_json(path: Path) -> list:
    if not path.exists():
        print(f"Missing {path}. Run main.py first to produce scraper output.")
        sys.exit(1)
    with path.open("r", encoding="utf-8") as f:
        data = json.load(f)
    if not isinstance(data, list):
        print(f"{path} must contain a JSON array, got {type(data).__name__}.")
        sys.exit(1)
    return data


def main() -> None:
    args = parse_args()

    targets = {}
    if args.collection in ("places", "both"):
        targets["places"] = load_json(args.places)
    if args.collection in ("events", "both"):
        targets["events"] = load_json(args.events)

    print("Targets:")
    for name, docs in targets.items():
        print(f"  {name}: {len(docs)} docs")

    if args.dry_run:
        print("Dry run — exiting before any Firestore writes.")
        return

    if not args.key.exists():
        print(f"Service account key not found at {args.key}.")
        print("Download from Firebase Console:")
        print("  https://console.firebase.google.com/project/gusur-57e95/settings/serviceaccounts/adminsdk")
        print(f"Save it to {args.key} (or pass --key /path/to/key.json).")
        sys.exit(1)

    cred = credentials.Certificate(str(args.key))
    firebase_admin.initialize_app(cred)
    db = firestore.client()

    print("Uploading to Firestore...")
    for name, docs in targets.items():
        upload_collection(db, name, docs)
    print("Done.")


if __name__ == "__main__":
    main()