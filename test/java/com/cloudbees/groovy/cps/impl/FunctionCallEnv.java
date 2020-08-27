package com.cloudbees.groovy.cps.impl;

import com.cloudbees.groovy.cps.Continuation;
import com.cloudbees.groovy.cps.Env;
import com.google.common.collect.Maps;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.cloudbees.groovy.cps.sandbox.Invoker;
import com.cloudbees.groovy.cps.sandbox.DefaultInvoker;
import com.cloudbees.groovy.cps.sandbox.SandboxInvoker;
import com.griddynamics.devops.mpl.testing.MPLDefaultInvoker;
import com.griddynamics.devops.mpl.testing.MPLSandboxInvoker;

/**
 * @author Kohsuke Kawaguchi
 */
// TODO: should be package local once all the impls move into this class
public class FunctionCallEnv extends CallEnv {
    /** To conserve memory, lazily declared using {@link Collections#EMPTY_MAP} until we declare variables, then converted to a (small) {@link HashMap} */
    Map<String,Object> locals;

    /**
     * @param caller
     *      The environment of the call site. Can be null but only if the caller is outside CPS execution.
     */
    public FunctionCallEnv(Env caller, Continuation returnAddress, SourceLocation loc, Object _this) {
        this(caller, returnAddress, loc, _this, 0);
    }

    public FunctionCallEnv(Env caller, Continuation returnAddress, SourceLocation loc, Object _this, int localsCount) {
        super(caller,returnAddress,loc, localsCount);
        if( caller != null )
            setInvoker(caller.getInvoker());
        else
            setInvoker(null);
        locals = (localsCount <= 0) ? new HashMap<String, Object>(2) : Maps.<String,Object>newHashMapWithExpectedSize(localsCount+1);
        locals.put("this", _this);
    }

    public void declareVariable(Class type, String name) {
        locals.put(name, null);
        getTypes().put(name, type);
    }

    public Object getLocalVariable(String name) {
        return locals.get(name);
    }

    public void setLocalVariable(String name, Object value) {
        locals.put(name,value);
    }

    public Object closureOwner() {
        return getLocalVariable("this");
    }

    @Override
    public void setInvoker(Invoker invoker) {
        if( invoker instanceof SandboxInvoker )
            super.setInvoker(new MPLSandboxInvoker());
        else
            super.setInvoker(new MPLDefaultInvoker());
    }

    private static final long serialVersionUID = 1L;
}
