package com.surroundfm.surroundfm.app;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.util.HashMap;

/**
 * Created on 5/2/14.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class User {
    @JsonUnwrapped

    @JsonProperty("_id")
    private String username;
    @JsonProperty("password")
    private String password;
    private String cookie;
    @JsonProperty("avatar")
    private String avatar;
    @JsonProperty("email")
    private String email;
    @JsonProperty("coordinates")
    private HashMap<String, Double> coordinates;
    @JsonProperty("zipcode")
    private String zipcode;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCookie() {
        return cookie;
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }
    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public HashMap<String, Double> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(HashMap<String, Double> coordinates) {
        this.coordinates = coordinates;
    }

    public String getZipcode() {
        return zipcode;
    }

    public void setZipcode(String zipcode) {
        this.zipcode = zipcode;
    }

    public String toString() {
        return "User " + this.username +
                " with cookie " + this.cookie +
                " with avatar " + this.avatar +
                " with email " + this.email +
                " with coordinates {" + this.coordinates.get("latitude") + ", " + this.coordinates.get("longitude") + "}";
    }
}
