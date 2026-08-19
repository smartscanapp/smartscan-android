## v2.0.0 - 19/08/2026

### Added
* Added concepts feature
* Added recent searches
* Added ability to manually add/edit media descriptions in MediaViewer
* Added ability to generate image descriptions (requires api key)
* Added support from deleting media from device
* Added setting to add OpenAI API key

### Changed
* Improved filtering and sorting
* Improved MediaViewer design
* Improved search by making it uses media descriptions which handles text-heavy images and video searches better
* Improved auto collection adaptability
* Created core (offline) and cloud modules

### Removed
* Removed strictness and auto open in gallery settings

___

## v1.3.4 - 08/07/2026

### Added
* Add support for quantized embeddings, reducing memory usage by 4x

### Changed
* Updated `MediaMetadata` table to use composite keys (id, type) to prevent silent collision between image and video MediaStore ids, which could have resulted in some videos not be included in scans.
* Do not reset selection after tagging to avoid having to reselect if adding multiple tags
* Made search results more responsive to strictness
* Remove support for backups that used old data - users must make new backup if needed (breaking)

___

## v1.3.3 - 15/06/2026

### Added
* Added media type filter for collections

### Changed
* Replaced similarity threshold with strictness setting as results are no longer ranked purely by similarity

### Fixed
* Fixed bugs related to allowed folders
* Fixed potential index file corruption bugs by using staged writes and only saving during sync if required

___

## v1.3.2 - 07/06/2026

### Added
* Added "Hide duplicates" setting to hide near-identical search results
* Added ability to merge Auto Collections
* Added ability to move media in Auto Collections
* Added support for mixed-media collections
* Added Select All for search results and collections
* Added re-ranked search results

### Changed
* Re-designed settings screen

### Fixed
* Fixed allowed folders bug caused by using allowed image folders instead of video folders when querying videos

___

## v1.3.1 - 01/05/2026
* Added ability to copy from multiple collections to a tag collection
* Added date filters for search
* Added media metadata table
* Added ability to rebuild index from scratch
* Added Koin DI

## Changed
* Synchronise embedding stores and media metadata table
* Handle top-level app logic in MainViewModel via prepareApp method
* Removed type from crossrefs

## Fixed
* Fixed tag-filtered queries missing results
* Fixed corrupted index files caused by race conditions issues, by moving sole responsibility of embed store saving to MainViewModal and supporting rebuild if required.

## Removed
* Removed cluster-based retrieval

___

## v1.3.0 - 19/04/2026

### Added
* Added cluster-based search improving search speed and relevance
* Added collections (Tag Collections and Auto Collections)
* Added ability to merge collections (Tag Collections only)
* Added bulk copy from Auto Collections to Tag Collections
* Added setting to control number of results shown per grid row
* Added setting to control similarity threshold for image queries
* Added swipe gestures to MediaViewer to swipe through media items

### Changed
* Single unified DB for both images and videos

### Removed
* Removed index frequency setting (defaults to daily updates)
* Removed tag suggestions when tagging selected items

---

## v1.2.0 - 27/12/2025 

### Added 
* Added image expansion in selection mode.

### Fixed
* Fixed crash when clicking a search result that was deleted after results were displayed.
* Fixed issue where external search would re-trigger when the Search screen was recomposed.
* Other minor UI improvements.

---

## v1.1.9 - 24/12/2025

### Fixed
* Fixed UI bug where all results for tag only search were not viewable
* Fixed other minor UI bugs

---

## v1.1.8 - 18/12/2025

### Added
* Added multi-select actions
* Added tagging support with auto-suggestions and auto-complete
* Added setting to auto-open results in gallery
* Search from other apps via Share/intent

### Changed 
* Auto-search when image is uploaded or selected

---

## v1.1.7 – 27/10/2025

### Added
* Reverse image search by uploading images directly from the search bar
* Reverse image search by pasting images into the search bar
* Reverse image search by long-pressing a result or using the Media Viewer
* Added new menu with “Refresh index” options for images and videos
* Added backup and restore settings

### Removed
* Removed auto-organisation feature

---

## v1.1.6 – 14/10/2025

### Added
* Added searching with images
* Select searchable folders for images and videos
* Unlimited search results
* Add model downloading and importing
* Updated search screen layout
* Added theme related settings

### Fixed
* Fixed auto-organisation bugs

---

## v1.1.5 – 26/09/2025

### Fixed
- Fixed bug that caused full re-indexing and duplication

---

## v1.1.4 – 24/09/2025

### Added
- Integrated SmartScan SDK
- Copy to clipboard in media viewer
- SmartScan subreddit link in settings
- Added organiser accuracy setting
- Updates pop up

### Changed
- Minor UI changes

---

## v1.1.3 – 20/08/2025

### Changed
- Replace Room DB with file-based index storage for faster loading
- Minor UI changes

---

## v1.1.2 – 08/08/2025

### Changed
- Clean, intuitive search UI with click-to-open media viewer and sharing features

---

## v1.1.1 – 11/06/2025

### Changed

* Improved memory efficiency in video processing

### Fixed

* UI freeze when selecting destination folders with many images
* Out-of-memory (OOM) crashes when processing/loading large images
* Double ONNX environment teardown bug

---

## v1.1.0 – 19/05/2025

### Added
- Video search support
- Undo last scan feature
- Help screen for guidance and troubleshooting

### Changed
- Refresh logic revised to avoid hard reset

### Fixed
- Reduced false positives in auto-organisation

---

## v1.0.6 – 30/04/2025

### Added
- Option to configure index frequency (daily or weekly) in Settings  
- Option to configure similarity threshold for search in Settings  

### Changed
- "Enable scanning" renamed to "Enable auto-organisation" for clarity
- Minor UI updates

### Removed
- Unnecessary network info permission  

---

## v1.0.5 – 13/04/2025

### Added
- Progress bar for indexing
- Indicator shown when background auto-organisation is running
- Expandable main search result
- Grid column layout for search results
- Enter key to search

### Changed
- Dynamic concurrency for memory management
- Batching implemented for organisation
- Skip images that have already been processed when organizing
- More robust and user-friendly error handling for background jobs

### Fixed
- Text visibility in light mode on search screen
- Fixed scan history not updating

---

## v1.0.4 – 03/04/2025

- Chained image index workers

## v1.0.3 – 03/04/2025

- Delete `image_embeddings` db when refreshing image index
- Remove battery constraint on image indexer worker
- Chained image index workers

---

## v1.0.2 – 03/04/2025

- Fix search bug that occurred due to changes in storage permissions
- Added new feature that allows refreshing image index to handle changes in storage permissions
- Fix bug that caused some files to be skipped in classification worker
- Memory optimizations

---

## v1.0.1 – 27/03/2025

- Updated `build.gradle` for compatibility with F-Droid reproducible builds  
- Updated app version display in the Settings screen  
- Made the Setting Details screen scrollable
