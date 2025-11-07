# VolunConnect: A Smart Volunteer Platform 

VolunConnect is a modern Android application built with Kotlin, designed to seamlessly connect community organizations with local volunteers. The app features a dual-role system (Volunteer and Organizer), a map-first discovery interface, and a robust backend powered by Firebase.

![login](https://github.com/user-attachments/assets/fc99053c-a841-4073-a17a-7c244dbbbdf3)
![signup](https://github.com/user-attachments/assets/2c3e9467-f769-4e3e-9623-54bc40ed57ed)
![hero](https://github.com/user-attachments/assets/06e07899-7403-430e-836f-0d3737a28658)



---

## Core Features

### For Volunteers
* **Map-First Discovery**: Find nearby volunteer opportunities on an interactive OpenStreetMap (OSMDroid) interface with color-coded markers for different event categories.
* **Event Details**: Tap any event to view a full detail screen, including descriptions, "what to bring," live registration counts, and one-click directions.
* **Smart Filtering**: Filter events by category (Environment, Education, Health, etc.) with real-time updates on the map and list.
* **Calendar Integration**: Add events directly to your device's calendar with a single tap.
* **Case-Insensitive Search**: Find specific events by title or organization name easily.
* **Secure Registration**: Simple and secure event registration flow with emergency contact details.

![feature1](https://github.com/user-attachments/assets/98d77368-134a-45b5-8f13-f878d773d366)
![register](https://github.com/user-attachments/assets/7451a8f9-6e54-4dfd-a7ba-27b21bc37b13)


### For Organizers
* **Dedicated Dashboard**: manage all your created events in one place.
* **Interactive Event Creation**: Create events by pinpointing the **exact location on a map** instead of just typing an address.
* **Track Registrations**: View a real-time list of all volunteers who have registered for your events.
* **Role-Based Access**: Secure login ensures only approved organizers can access these features.

![organiser1](https://github.com/user-attachments/assets/e0686a67-1e83-49a7-a6d4-6900f6a7b81f)
![organiser2](https://github.com/user-attachments/assets/f768b8b8-2a1f-478a-a96c-c59ac54e6f55)


---

## Tech Stack & Key Libraries

This project was built using modern Android development practices and a robust serverless backend.

### Frontend (Android)
* **Language**: **Kotlin**
* **UI**: **XML** with **Material Design 3** Components (`MaterialCardView`, `ChipGroup`, `BottomSheet`)
* **Architecture**: Activity-based architecture with `RecyclerView` for list management.
* **Maps & Location**:
    * **OSMDroid**: A free, open-source library for the interactive map, custom markers, and legend.
    * **Google Play Services Location**: Used via `FusedLocationProviderClient` to fetch the user's current GPS location.
* **Settings**:
    * **AndroidX Preference**: Used to create the entire "Settings" screen for theme switching and user logout.

### Backend (Firebase)
* **Authentication**: Email/Password.
* **Firestore (NoSQL Database)**: Primary database for all app data, utilizing complex compound queries and indexes for performance.

---

## Backend Architecture

The app uses a relational data model within Firestore's NoSQL structure:

* **`users` Collection**: Stores user profiles and roles (Volunteer vs. Organizer). Linked via Firebase UID.
* **`events` Collection**: Stores all event data, including geospatial coordinates (`GeoPoint`) and organizer details.
* **`registrations` Collection**: Acts as a junction table linking a specific `Volunteer` to a specific `Event`.
### Smart Backend Logic
This project's backend isn't just a simple data store; it includes specific logic:

* **Role-Based Redirect**: On login, the app checks the `role` field in the user's Firestore document to dynamically redirect them to the correct dashboard (Volunteer or Organizer).
* **Case-Insensitive Search**: To make searching user-friendly, the app automatically saves lowercase versions of key fields (e.g., `title_lowercase`). The search function then queries these lowercase fields, allowing a user to find "Marine Drive" by typing "marine".
* **Live Aggregation**: The app uses Firestore's `.count()` aggregation to efficiently get the *real-time* number of registered volunteers for an event, rather than downloading the entire list.
---

## How to Run This Project

To run this project locally, you must connect it to your own Firebase project.

1.  **Clone the Repository**
    ```bash
    git clone [https://github.com/your-username/VolunConnect.git]
    ```

2.  **Firebase Setup (Crucial Step)**
    * Create a new project on the [Firebase Console](https://console.firebase.google.com/).
    * **Enable Authentication**: Turn on Email/Password and Google Sign-In providers.
    * **Enable Firestore**: Create a database in **Test Mode**.
    * **Add Android App**: Register an app with the package name `com.example.voluntra_mad_project`.
    * **Download Config**: Download the `google-services.json` file and place it in the `app/` directory of this project.

3.  **Create Firestore Indexes (Required)**
    * Your app will fail to load filtered data until you create the necessary indexes. The easiest way to do this is to:
    1.  Run the app and log in.
    2.  Try to **filter events** on the volunteer dashboard.
    3.  Try to **view the organizer dashboard**.
    4.  Check your **Logcat** in Android Studio for a red error message that begins with `FAILED_PRECONDITION: The query requires an index...`
    5.  **Click the URL** provided in the error log. It will open the Firebase Console and auto-fill the correct fields for the index.
    6.  Click **"Create Index"** and wait for it to build (this can take a few minutes).
    7.  Repeat this process for all three required indexes (volunteer filter, organizer dashboard, and view registrations).

4.  **Build & Run**
    * Open the project in Android Studio and let Gradle sync.
    * Run the app on an emulator or physical device.
    *(Note: The first time you try to filter or view organizer events, check Logcat for a link to auto-create the necessary Firestore Indexes).*
