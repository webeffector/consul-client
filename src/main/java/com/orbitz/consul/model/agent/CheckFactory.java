package com.orbitz.consul.model.agent;

/**
 * @author Sergey Zudin
 * @since 25.02.15.
 */
public class CheckFactory {
    public static Registration.Check createTtlCheck(long ttl) {
        Registration.TtlCheck check = new Registration.TtlCheck();
        check.setTtl(String.format("%ss", ttl));
        return check;
    }

    public static Registration.Check createScriptCheck(String script, long interval) {
        Registration.ScriptCheck check = new Registration.ScriptCheck();
        check.setScript(script);
        check.setInterval(String.format("%ss", interval));
        return check;
    }

    public static Registration.Check createHttpCheck(String http, long interval) {
        Registration.HttpCheck check = new Registration.HttpCheck();
        check.setHttp(http);
        check.setInterval(String.format("%ss", interval));
        return check;
    }
}
