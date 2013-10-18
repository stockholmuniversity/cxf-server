package se.su.it.svc.aspect;

import org.apache.cxf.phase.PhaseInterceptorChain;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Before;
import org.slf4j.LoggerFactory;
import se.su.it.svc.annotations.AuditHideReturnValue;
import se.su.it.svc.annotations.AuditMethodDetails;
import se.su.it.svc.audit.AuditEntity;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class AuditAspect {
  private static final String STATE_INPROGRESS = "IN PROGRESS";
  private static final String STATE_SUCCESS = "SUCCESS";
  private static final String STATE_EXCEPTION = "EXCEPTION";
  private static final String UNKNOWN = "<unknown>";
  private static final String HIDDEN_VALUE = "******";

  static final org.slf4j.Logger logger = LoggerFactory.getLogger(AuthorizorAspect.class);

  @Before("execution(* (@javax.jws.WebService *).*(..))")
  public void auditBefore(JoinPoint joinPoint) throws Throwable {
    String id = getId();
    Class targetClass = joinPoint.getTarget().getClass();
    String methodName = joinPoint.getSignature().getName();
    Object[] args = joinPoint.getArgs();

    logger.info("["+id+"] Before: " + targetClass.getName() + "." + methodName + " with " + args.length + "params");

    Method method = null;
    try {
       method = getMethod(targetClass, methodName, args);
    } catch (NoSuchMethodException e) {
      logger.warn("["+id+"] Could not get method for " + targetClass.getName() + "." + methodName);
    }

    // Serialize the argument Object list into a ByteArray
    ByteArrayOutputStream bsArgs = new ByteArrayOutputStream();
    ObjectOutputStream outArgs = new ObjectOutputStream(bsArgs);
    outArgs.writeObject(args);
    outArgs.close();

    // Create an AuditEntity based on the gathered information
    AuditEntity ae = AuditEntity.getInstance(
            new Timestamp(new Date().getTime()).toString(),
            methodName,
            objectsToString(args),
            new String(bsArgs.toByteArray()),
            UNKNOWN,
            UNKNOWN,
            STATE_INPROGRESS,
            getMethodDetails(method)
    );

    logger.info("["+id+"] Received: "+ ae);
  }

  @AfterReturning(
          pointcut = "execution(* (@javax.jws.WebService *).*(..))",
          returning = "result")
  public void auditAfterReturning(JoinPoint joinPoint, Object result) throws Throwable {
    String id = getId();
    Class targetClass = joinPoint.getTarget().getClass();
    String methodName = joinPoint.getSignature().getName();
    Object[] args = joinPoint.getArgs();

    logger.info("["+id+"] After: " + targetClass.getName() + "." + methodName + " with " + args.length + "params");

    Method method = null;
    try {
      method = getMethod(targetClass, methodName, args);
    } catch (NoSuchMethodException e) {
      logger.warn("["+id+"] Could not get method for " + targetClass.getName() + "." + methodName);
    }

    if (method != null && method.isAnnotationPresent(AuditHideReturnValue.class)) {
      result = HIDDEN_VALUE;
    }

    // Serialize the argument Object list into a ByteArray
    ByteArrayOutputStream bsArgs = new ByteArrayOutputStream();
    ObjectOutputStream outArgs = new ObjectOutputStream(bsArgs);
    outArgs.writeObject(args);
    outArgs.close();

    // Serialize the Return object into a ByteArray
    ByteArrayOutputStream bsRet = new ByteArrayOutputStream();
    ObjectOutputStream outRet = new ObjectOutputStream(bsRet);
    outRet.writeObject(result);
    outRet.close();

    AuditEntity ae = AuditEntity.getInstance(
            new Timestamp(new Date().getTime()).toString(),
            methodName,
            objectsToString(args),
            new String(bsArgs.toByteArray()),
            result != null ? result.toString() : null,
            new String(bsRet.toByteArray()),
            STATE_SUCCESS,
            getMethodDetails(method)
    );

    logger.info("["+id+"] Returned: " + ae);
  }

  @AfterThrowing(
          pointcut = "execution(* (@javax.jws.WebService *).*(..))",
          throwing = "throwable")
  public void auditAfterThrowing(JoinPoint joinPoint, Throwable throwable) throws Throwable {
    String id = getId();
    Class targetClass = joinPoint.getTarget().getClass();
    String methodName = joinPoint.getSignature().getName();
    Object[] args = joinPoint.getArgs();

    logger.info("["+id+"] After exception: " + targetClass.getName() + "." + methodName + " with " + args.length + "params");

    Method method = null;
    try {
      method = getMethod(targetClass, methodName, args);
    } catch (NoSuchMethodException e) {
      logger.warn("["+id+"] Could not get method for " + targetClass.getName() + "." + methodName);
    }

    // Serialize the argument Object list into a ByteArray
    ByteArrayOutputStream bsArgs = new ByteArrayOutputStream();
    ObjectOutputStream outArgs = new ObjectOutputStream(bsArgs);
    outArgs.writeObject(args);
    outArgs.close();

    AuditEntity ae = AuditEntity.getInstance(
            new Timestamp(new Date().getTime()).toString(),
            methodName,
            objectsToString(args),
            new String(bsArgs.toByteArray()),
            throwable != null ? throwable.toString() : null,
            UNKNOWN,
            STATE_EXCEPTION,
            getMethodDetails(method)
    );

    logger.info("["+id+"] Exception: " + ae);
  }

  protected String objectsToString(Object[] objects) {
    if (objects == null) {
      return "null";
    }

    StringBuilder sb = new StringBuilder();

    sb.append("[");
    for (Object object : objects) {
      sb.append(object).append(", ");
    }
    sb.replace(sb.lastIndexOf(","), sb.length(), "").append("]");

    return sb.toString();
  }

  protected String getId() {
    String id = "";

    try {
      HttpServletRequest request = (HttpServletRequest) PhaseInterceptorChain.getCurrentMessage().get("HTTP.REQUEST");
      id = request.getSession().getId();
    } catch (Exception ex) {
      logger.debug("Failed to get id from session", ex);
    }

    return id;
  }

  private Method getMethod(Class target, String name, Object[] args) throws NoSuchMethodException {
    Class[] parameterTypes = new Class[args.length];
    for(int i = 0; i < parameterTypes.length; i++) {
      parameterTypes[i] = args[i].getClass();
    }

    return target.getMethod(name, parameterTypes);
  }

  /**
   * Generate MethodDetails from annotation on method to be able to describe
   * functions that will be invoked by this method
   *
   * @return the method details
   */
  private List<String> getMethodDetails(Method method) {
    List<String> methodDetails = new ArrayList<String>();

    if (method != null) {
      if (method.isAnnotationPresent(AuditMethodDetails.class)) {
        AuditMethodDetails annotation = method.getAnnotation(AuditMethodDetails.class);
        String details = annotation.details();

        if (details != null) {
          Collections.addAll(methodDetails, details.split(","));
        }
      }
    }

    return methodDetails;
  }
}
