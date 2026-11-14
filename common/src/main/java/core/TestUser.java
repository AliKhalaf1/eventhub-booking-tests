package core;


public enum TestUser {

    MAIN("USER_EMAIL", "USER_PASSWORD"),

    SECOND("USER2_EMAIL", "USER2_PASSWORD");

    private final String emailKey;
    private final String passwordKey;

    TestUser(String emailKey, String passwordKey) {
        this.emailKey = emailKey;
        this.passwordKey = passwordKey;
    }

    public String email() {
        return Config.required(emailKey);
    }

    public String password() {
        return Config.required(passwordKey);
    }

    public boolean isConfigured() {
        return Config.get(emailKey).isPresent() && Config.get(passwordKey).isPresent();
    }

    public String configKeys() {
        return emailKey + " / " + passwordKey;
    }
}
