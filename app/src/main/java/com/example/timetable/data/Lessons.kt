package com.example.timetable.data


class Lessons (
    courseTitle: String,
    lessonRef: String,
    lessonBlock: String,
    courseRef: String,
    dates: String,
    startTime: String,
    endTime: String,
    subjectCode: String,
    classCodes: String,
    roomCodes: String?,
    buildingCodes: String?,
    changes: Changes?
)

class Changes (
    caption: String,
    changeType: Int,
    absentTeacherCodes: String?,
    reasonType: String?,
    lessonTitle: String,
    cancelled: String,
    modified: String
)