package fu.sep490.g23.backend.service.classroom;

/**
 * A provider-neutral recording ready for staff review and publication.
 */
public record VirtualMeetingRecordingInfo(
        String url,
        Long durationMs
) {
}
