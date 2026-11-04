package lab.pricing;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/** Counts every call to a method marked {@link Logged}. */
@Logged
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class LoggedInterceptor {

    @Inject
    CallCounter counter;

    @AroundInvoke
    Object count(InvocationContext context) throws Exception {
        counter.hit();
        return context.proceed();
    }
}
