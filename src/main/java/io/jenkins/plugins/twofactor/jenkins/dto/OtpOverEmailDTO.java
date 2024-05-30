package io.jenkins.plugins.twofactor.jenkins.dto;

public class OtpOverEmailDTO {
    private final boolean isEnabled;
    private final String senderEmailAddress;

    public OtpOverEmailDTO(boolean isEnabled, String senderEmailAddress) {
        this.isEnabled = isEnabled;
        this.senderEmailAddress = senderEmailAddress;
    }

    public boolean getIsEnabled() {
      return isEnabled;
    }

    public String getSenderEmailAddress() {
      return senderEmailAddress;
    }


    public static OtpOverEmailDTO getDefault() {
        return new OtpOverEmailDTO(false, "");
    }
}