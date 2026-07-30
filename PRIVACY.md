# Privacy

Kiosk is a private-by-default Hacker News reader.

Kiosk does not include:

- advertising SDKs
- analytics SDKs
- tracking SDKs
- Firebase
- Crashlytics
- Google Play Services
- user accounts

## Network Requests

Kiosk makes network requests for:

- Hacker News feed, story, and comment data from the official read-only Hacker News API.
- Historical story search through the unauthenticated Algolia Hacker News Search API.
- Article pages selected or warmed for reader extraction.
- Favicons and article images shown in the reader.
- External links only when the user opens them in another app or browser.

## Local Data

Kiosk stores local preferences such as reader appearance, Speed Reader settings, selected feed, and
opened story ids. It also stores cached reader extractions so articles reopen faster and can be
available without refetching the original page.

Kiosk does not send this local data to the developer.
