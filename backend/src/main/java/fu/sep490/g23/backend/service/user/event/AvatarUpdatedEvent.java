package fu.sep490.g23.backend.service.user.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Published after a user avatar is updated and the database transaction has committed.
 * The listener deletes the previous avatar file from the object store.
 */
@Getter
@RequiredArgsConstructor
public class AvatarUpdatedEvent {
    /** The R2 key of the previous avatar (e.g. "avatars/avatar-xxx.jpg"). Null if there was no previous avatar. */
    private final String previousAvatarKey;
}
