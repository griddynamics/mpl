//
// Copyright (c) 2020 Grid Dynamics International, Inc. All Rights Reserved
// https://www.griddynamics.com
//
// Classification level: Public
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// $Id: $
// @Project:     MPL
// @Description: Shared Jenkins Modular Pipeline Library
//

package com.griddynamics.devops.mpl.testing

import com.griddynamics.devops.mpl.testing.MPLMethodCall
import com.griddynamics.devops.mpl.testing.MPLMethodNotAllowedException
import com.lesfurets.jenkins.unit.MethodSignature
import static com.lesfurets.jenkins.unit.MethodSignature.method

import org.codehaus.groovy.runtime.MetaClassHelper

import com.cloudbees.groovy.cps.SerializableScript
import com.cloudbees.groovy.cps.impl.CpsClosure
import org.jenkinsci.plugins.workflow.cps.CpsThread
import org.jenkinsci.plugins.workflow.cps.CpsThreadGroup
import org.jenkinsci.plugins.workflow.graph.BlockStartNode

/**
 * Stores all the logic to intercept groovy-cps Invoker methods
 * Allows to trace the calls, replace arguments of specified functions
 *
 * @author Sergei Parshev <sparshev@griddynamics.com>
 */
@Singleton
class MPLInterceptor {
  public boolean DEBUG_SHOW_METHODCALL_SKIPPING = false

  protected List<Class> allowed_scopes = [SerializableScript.class, CpsClosure.class]

  protected Map<MethodSignature, Closure> allowed_methods = [:]

  protected List<StackTraceElement> registered_elements = []
  List<MPLMethodCall> call_stack = []

  protected Map arguments_replaces = [:]

  public boolean checkReceiverScope(Object receiver) {
    if( receiver instanceof Class )
      return receiver in allowed_scopes || receiver.getSuperclass() in allowed_scopes
    for( def clazz : allowed_scopes ) {
      if( clazz.isInstance(receiver) )
        return true
    }
    return false
  }

  public void registerAllowedMethod(String name, List<Class> args = [], Closure closure) {
    allowed_methods.put(method(name, args.toArray(new Class[args?.size()])), closure)
  }

  public void registerAllowedMethod(MethodSignature methodSignature, Closure closure) {
    allowed_methods.put(methodSignature, closure)
  }

  public Object processMethodCall(Closure callback, String invoker, Object receiver, String name, Object... args) {
    // The receiver should be in the testing scope
    if( ! checkReceiverScope(receiver) ) {
      if( Boolean.parseBoolean(System.getProperty('mpl.interceptor.show_methodcall_skipping')) ) {
        if( receiver instanceof Class )
          System.out.println("----> skipping ${invoker} MethodCall: static ${receiver}.${name} (${receiver.getSuperclass()})")
        else
          System.out.println("----> skipping ${invoker} MethodCall: ${receiver.class}.${name} (${receiver.class?.getSuperclass()})")
      }
      return callback(receiver, name, args)
    }

    // Put the method into the unit test stack
    args = registerMethodCall(invoker, receiver, name, args)

    // Check if it is in the allowed list
    def intercepted = getAllowedMethodEntry(name, args)
    if( intercepted == null ) // Method is not allowed
      throw new MPLMethodNotAllowedException(name, receiver.class, args)

    if( intercepted.value != null ) { // Allowed method closure specified
      intercepted.value.delegate = receiver instanceof CpsClosure ? receiver.delegate : receiver
      return callClosure(intercepted.value, args)
    }

    // Executing the original allowed method
    return callback(receiver, name, args)
  }

  public List registerMethodCall(String invoker, Object receiver, String name, Object... args) {
    if( Boolean.parseBoolean(System.getProperty('mpl.interceptor.show_methodcall_registering')) ) {
      if( receiver instanceof Class )
        System.out.println("----> registering ${invoker} MethodCall: static ${receiver}.${name} (${receiver.getSuperclass()})")
      else
        System.out.println("----> registering ${invoker} MethodCall: ${receiver.class}.${name} (${receiver.class?.getSuperclass()})")
    }

    // To produce better stacktrace view - using FlowNode blocks
    int depth = CpsThread.current().head.get().getEnclosingBlocks().size()
    // Adding the current block if it's a start node
    depth += CpsThread.current().head.get() instanceof BlockStartNode ? 1 : 0

    // This ways to determine the depth is not working correctly, but probably
    // better if we need to test deeper than CpsScript / CpsClosure
    // * Thread will not wirk well with the block steps closures
    //     int depth = CpsThread.current().getStackTrace().size()
    // * Using Group is better - but still not working with MPLModule well
    //   and adding an extra depth level after each MPLModule call...
    //     int depth = CpsThreadGroup.current().getThreadDump().getThreads()
    //       .collect { it.getStackTrace() }.flatten().size()

    def call = new MPLMethodCall()
    call.invoker = invoker
    call.target = receiver instanceof CpsClosure ? receiver.delegate : receiver
    call.methodName = name
    call.args = args
    call.args = pullArgumentsReplace(call.toString(), args)
    call.stackDepth = depth
    call_stack.add(call)

    return call.args
  }

  public void addArgumentsReplace(String method_call_id, Object... args) {
    arguments_replaces[method_call_id] = args
  }

  public List pullArgumentsReplace(String method_call_id, Object... orig_args) {
    if( Boolean.parseBoolean(System.getProperty('mpl.interceptor.show_methodcall_to_replace')) )
      System.out.println("----> Method to replace: '${method_call_id}'")
    return arguments_replaces.remove(method_call_id) ?: orig_args
  }

  public Map.Entry<MethodSignature, Closure> getAllowedMethodEntry(String name, Object... args) {
    Class[] paramTypes = MetaClassHelper.castArgumentsToClassArray(args)
    MethodSignature signature = method(name, paramTypes)
    return allowed_methods.find { k, v -> k == signature }
  }

  public Object callClosure(Closure closure, Object[] args = null) {
    if( !args )
      return closure.call()
    else if( args.size() > closure.maximumNumberOfParameters )
      return closure.call(args)
    else
      return closure.call(*args)
  }

  public void printCallStack() {
    if( !Boolean.parseBoolean(System.getProperty('mpl.interceptor.printstack', 'true')) )
      return

    call_stack.each { println(it) }
  }

  public void clear() {
    call_stack.clear()
  }
}
