package com.aq.aqconnect;

/**
 *
 * @author Aditya TJ
 */
public class AQPluginException extends RuntimeException {

    public AQPluginException(String message) {
        super(String.format("[%s]", message));
    }
}