package com.storemanager.core.runtime.async;

import com.storemanager.core.session.AppSession;
import com.storemanager.domain.user.model.User;
import java.util.concurrent.Callable;

/** Identity captured when work is submitted; never changes authorization or the login session. */
public final class AuditActorContext {
    public record Actor(Long userId, String username) { }
    private static final ThreadLocal<Actor> CURRENT = new ThreadLocal<>();

    private AuditActorContext() { }

    public static Actor capture() {
        Actor actor = CURRENT.get();
        if (actor != null) return actor;
        User user = AppSession.getCurrentUser();
        return user == null ? new Actor(null, "System") : new Actor(user.getId(), user.getUsername());
    }

    public static <T> T callAs(Actor actor, Callable<T> task) throws Exception {
        Actor previous = CURRENT.get();
        CURRENT.set(actor);
        try {
            return task.call();
        } finally {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
    }
}
