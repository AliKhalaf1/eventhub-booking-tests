## Issues with the story

### 1. Who can book?
The story is written for a "visitor" but we need a logged in user in order to show the events, so it makes more sense that that only a logged in user is allowed to book. But is that the requirement?

### 2. One attendee or several?
The story says "I want to book tickets … so that **I** can attend", which describes one person. But the app lets you book several tickets in one booking. and if multiple is tickets per user is allowed do we need to details for each attendee?


### 3. What are the conditions for booking?
The story doesn't give any limits. The app allows 1 to 10 tickets per booking, but nothing in the story says so. It might be safe to assume 1 as the minimum but a maximum boundry needs to be set


### 4. Does the booking reference need a specific format?
AC1 only says "a booking reference". The API docs give the format `EVT-XXXXXX`, but the app returns references like `H-9CR5WD` so what format should the reference follow, and must it be unique?


## Issues found

### Cancelling a booking deletes it
The API docs describe a `cancelled` booking status so a cancelled booking should be kept and marked as cancelled.

This is probably not a real issue but just to clean the data as this is a shared env
