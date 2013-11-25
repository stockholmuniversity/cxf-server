/*
 * Copyright (c) 2013, IT Services, Stockholm University
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of Stockholm University nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package se.su.it.svc.server.aspect;

import org.apache.cxf.phase.PhaseInterceptorChain;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.LoggerFactory;
import se.su.it.svc.server.annotations.AuthzRole;
import se.su.it.svc.server.security.Authorizor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;

@Aspect
public class AuthorizorAspect {

  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(AuthorizorAspect.class);

  private Authorizor authorizor;

  @Around("execution(* (@se.su.it.svc.server.annotations.AuthzRole *).*(..))")
  public Object withClassAnnotation(ProceedingJoinPoint joinPoint) throws Throwable {
    Class target = joinPoint.getTarget().getClass();
    LOG.debug("Intercepted method " + target.getName() + "." + joinPoint.getSignature().getName());

    Annotation annotation = target.getAnnotation(AuthzRole.class);
    String role = null;
    if (annotation != null) {
      role = ((AuthzRole) annotation).role();
    }

    return handleAspect(joinPoint, role);
  }

  @Around("execution(@se.su.it.svc.server.annotations.AuthzRole * *(..)) && @annotation(annotation)")
  public Object withMethodAnnotation(ProceedingJoinPoint joinPoint, AuthzRole annotation) throws Throwable {
    LOG.debug("Intercepted method " + joinPoint.getTarget().getClass().getName() + "." + joinPoint.getSignature().getName());
    String role = annotation.role();

    return handleAspect(joinPoint, role);
  }

  /**
   * Set a authorizer for this aspect.
   *
   * @param authorizor the authorizer to use.
   */
  public void setAuthorizor(Authorizor authorizor) {
    this.authorizor = authorizor;
  }

  /**
   * Run the handle the authorization.
   *
   * @param joinPoint
   * @param role
   * @return
   * @throws Throwable
   */
  private Object handleAspect(ProceedingJoinPoint joinPoint, String role) throws Throwable {
    Object result = null;
    HttpServletRequest httpServletRequest = (HttpServletRequest) PhaseInterceptorChain.getCurrentMessage().get("HTTP.REQUEST");
    String uid = httpServletRequest.getRemoteUser();

    LOG.debug("Running Authorizor.checkRole for uid=" + uid + ", role=" + role);

    if (authorizor == null || authorizor.checkRole(uid, role)) {
      LOG.info("Authorizor.checkRole for uid=" + uid + ", role=" + role + ": OK");
      result = joinPoint.proceed();
    } else {
      LOG.info("Authorizor.checkRole for uid=" + uid + ", role=" + role + ": DENIED");
      HttpServletResponse httpServletResponse = (HttpServletResponse) PhaseInterceptorChain.getCurrentMessage().get("HTTP.RESPONSE");
      httpServletResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "You do not have the the required role '" + role + "'");
    }

    return result;
  }
}
