package lab.melostudy;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Audited
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class AuditInterceptor {

    @Inject
    AuditLog audit;

    @AroundInvoke
    Object audit(InvocationContext context) throws Exception {
        audit.record(context.getMethod().getDeclaringClass().getSimpleName()
                + "." + context.getMethod().getName());
        return context.proceed();
    }
}
