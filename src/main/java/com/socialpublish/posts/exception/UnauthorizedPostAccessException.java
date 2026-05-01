package com.socialpublish.posts.exception;

public class UnauthorizedPostAccessException extends RuntimeException {

    public UnauthorizedPostAccessException() {
        super("You do not have access to this post");
    }
}
