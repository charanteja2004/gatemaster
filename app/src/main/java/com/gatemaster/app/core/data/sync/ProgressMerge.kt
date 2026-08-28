package com.gatemaster.app.core.data.sync

import com.gatemaster.app.core.data.TopicProgress

/**
 * Merging one device's reading history with another's.
 *
 * This is the part of sync with an actual decision in it. The server stores the
 * document whole and never looks inside, so the rule for what happens when two
 * phones have both been read on lives here, on the client, and it is a pure
 * function so it can be tested exhaustively without a server or a device.
 *
 * The rule, per field, and why:
 *
 * - **`furthest` takes the maximum.** Reading is cumulative. If the tablet got
 *   to 60% and the phone to 30%, the answer is 60% -- taking the later write
 *   instead would un-read half a chapter because the phone was opened last.
 * - **`lastOpenedEpochMs` takes the maximum**, since it is a high-water mark by
 *   definition.
 * - **`bookmarked` follows whichever record was opened more recently.** It is
 *   the one field a user genuinely toggles both ways, so neither OR nor AND is
 *   right: OR could never remove a bookmark, AND could never keep one. The more
 *   recently touched device is the best available evidence of intent.
 * - **Everything else -- title, path, subject -- comes from the more recent
 *   record**, because those change only when the content itself is rebuilt, and
 *   the newer record was written against the newer content.
 *
 * Ties go to local. They only happen when both sides are the same record.
 */
fun mergeProgress(
    local: Map<String, TopicProgress>,
    remote: Map<String, TopicProgress>,
): Map<String, TopicProgress> {
    if (remote.isEmpty()) return local
    if (local.isEmpty()) return remote

    val merged = LinkedHashMap<String, TopicProgress>(local.size + remote.size)
    merged.putAll(local)

    for ((topicId, theirs) in remote) {
        val ours = merged[topicId]
        merged[topicId] = if (ours == null) theirs else mergeTopic(ours, theirs)
    }
    return merged
}

private fun mergeTopic(ours: TopicProgress, theirs: TopicProgress): TopicProgress {
    // Which record is the more recent one, for the fields where recency is the
    // tie-breaker rather than the maximum.
    val newer = if (theirs.lastOpenedEpochMs > ours.lastOpenedEpochMs) theirs else ours

    return newer.copy(
        furthest = maxOf(ours.furthest, theirs.furthest),
        lastOpenedEpochMs = maxOf(ours.lastOpenedEpochMs, theirs.lastOpenedEpochMs),
        bookmarked = newer.bookmarked,
    )
}
