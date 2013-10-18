package se.su.it.svc.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.slf4j.LoggerFactory;


public class SanitizeWebParametersAspect {

  static final org.slf4j.Logger logger = LoggerFactory.getLogger(SanitizeWebParametersAspect.class);

  @Around("execution(* (@javax.jws.WebService *).*(..))")
  Object runAspect(ProceedingJoinPoint joinPoint) throws Throwable {
    Object[] args = joinPoint.getArgs();

    logger.debug("Intercepted method " + joinPoint.getTarget().getClass().getName() + "." + joinPoint.getSignature().getName());

    try {
      args = washArgs(args);
    } catch (Exception ex) {
      logger.error("Failed to sanitize arguments for method ${method.name}, attributes supplied were: ${args.join(", ")}", ex);
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
