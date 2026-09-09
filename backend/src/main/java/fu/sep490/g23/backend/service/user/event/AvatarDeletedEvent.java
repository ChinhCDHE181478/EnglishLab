package fu.sep490.g23.backend.service.user.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Published after a user avatar is deleted and the database transaction has committed.
 * The listener deletes the avatar file from the object store.
 */
@Getter
@RequiredArgsConstructor
public class AvatarDeletedEvent {
    /** The R2 key of the deleted avatar (e.g. "avatars/avatar-xxx.jpg"). */
    private final String avatarKey;
}
