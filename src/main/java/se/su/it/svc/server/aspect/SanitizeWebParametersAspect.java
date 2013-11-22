package se.su.it.svc.server.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.LoggerFactory;

@Aspect
public class SanitizeWebParametersAspect {

  static final org.slf4j.Logger LOG = LoggerFactory.getLogger(SanitizeWebParametersAspect.class);

  @Around("execution(* (@javax.jws.WebService *).*(..))")
  public Object runAspect(ProceedingJoinPoint joinPoint) throws Throwable {
    Object[] args = joinPoint.getArgs();

    LOG.debug("Intercepted method " + joinPoint.getTarget().getClass().getName() + "." + joinPoint.getSignature().getName());

    try {
      args = washArgs(args);
    } catch (Exception ex) {
      LOG.error("Failed to sanitize arguments for method ${method.name}, attributes supplied were: ${args.join(", ")}", ex);
    }

    return joinPoint.proceed(args);
  }

  private static Object[] washArgs(Object[] args) {
    Object[] washedArgs = new Object[args.length];

    for (int i = 0; i < args.length; i++) {
      Object arg = args[i];
      if (arg instanceof String) {
        washedArgs[i] = ((String)arg).trim();
      } else {
        washedArgs[i] = arg;
      }
    }
    return washedArgs;
  }
}
