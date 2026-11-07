package com.fongmi.android.tv.bean;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.gson.HeaderAdapter;
import com.fongmi.android.tv.gson.MsgAdapter;
import com.fongmi.android.tv.gson.UrlAdapter;
import com.github.catvod.utils.Auth;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

public class Result implements Parcelable {

    @SerializedName("url")
    @JsonAdapter(UrlAdapter.class)
    private Url url;

    @SerializedName("header")
    @JsonAdapter(HeaderAdapter.class)
    private Map<String, String> header;

    private transient Auth.Context auth;

    @SerializedName("msg")
    @JsonAdapter(MsgAdapter.class)
    private String msg;

    @SerializedName("playUrl")
    private String playUrl;
    @SerializedName("format")
    private String format;
    @SerializedName("click")
    private String click;
    @SerializedName("key")
    private String key;
    @SerializedName("position")
    private Long position;
    @SerializedName("parse")
    private Integer parse;
    @SerializedName("code")
    private Integer code;
    @SerializedName("drm")
    private Drm drm;
    @SerializedName("timeshift")
    private Timeshift timeshift;

    public Result() {
    }

    protected Result(Parcel in) {
    }

    public static Result objectFrom(String str) {
        try {
            return App.gson().fromJson(str, Result.class);
        } catch (Exception e) {
            return empty();
        }
    }

    public static Result fromJson(String str) {
        Result result = objectFrom(str);
        return result == null ? empty() : result;
    }

    public static Result empty() {
        return new Result();
    }

    public static Result error(String msg) {
        Result result = new Result();
        result.setParse(0);
        result.setMsg(msg);
        return result;
    }

    public Url getUrl() {
        return url == null ? Url.create() : url;
    }

    public void setUrl(String url) {
        this.url = getUrl().replace(url);
    }

    public String getMsg() {
        return TextUtils.isEmpty(msg) || getCode() != 0 ? "" : msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Map<String, String> getHeader() {
        return header == null ? new HashMap<>() : header;
    }

    public Auth.Context getAuth() {
        return auth;
    }

    public void setAuth(Auth.Context auth) {
        this.auth = auth;
    }

    public void setHeader(Map<String, String> header) {
        if (getHeader().isEmpty()) this.header = header;
    }

    public void addHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) return;
        header = new HashMap<>(getHeader());
        header.putAll(headers);
    }

    public String getPlayUrl() {
        return TextUtils.isEmpty(playUrl) ? "" : playUrl;
    }

    public void setPlayUrl(String playUrl) {
        this.playUrl = playUrl;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getClick() {
        return TextUtils.isEmpty(click) ? "" : click;
    }

    public void setClick(String click) {
        this.click = click;
    }

    public String getKey() {
        return TextUtils.isEmpty(key) ? "" : key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Long getPosition() {
        return position;
    }

    public void setPosition(Long position) {
        this.position = position;
    }

    public Integer getParse() {
        return parse == null ? 0 : parse;
    }

    public void setParse(Integer parse) {
        this.parse = parse;
    }

    public Integer getCode() {
        return code == null ? 0 : code;
    }

    public Drm getDrm() {
        return drm;
    }

    public void setDrm(Drm drm) {
        this.drm = drm;
    }

    public Timeshift getTimeshift() {
        return timeshift;
    }

    public void setTimeshift(Timeshift timeshift) {
        this.timeshift = timeshift;
    }

    public boolean hasMsg() {
        return !getMsg().isEmpty();
    }

    public boolean hasPosition() {
        return getPosition() != null;
    }

    public String getRealUrl() {
        return getPlayUrl() + getUrl().v();
    }

    @NonNull
    @Override
    public String toString() {
        return App.gson().toJson(this);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }

    public static final Creator<Result> CREATOR = new Creator<>() {
        @Override
        public Result createFromParcel(Parcel source) {
            return new Result(source);
        }

        @Override
        public Result[] newArray(int size) {
            return new Result[size];
        }
    };
}
