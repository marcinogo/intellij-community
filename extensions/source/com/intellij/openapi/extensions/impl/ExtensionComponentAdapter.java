/*
 * Copyright 2000-2005 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.extensions.impl;

import com.intellij.openapi.extensions.LoadingOrder;
import com.intellij.openapi.extensions.PluginAware;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.util.pico.AssignableToComponentAdapter;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.picocontainer.*;
import org.picocontainer.defaults.AssignabilityRegistrationException;
import org.picocontainer.defaults.CachingComponentAdapter;
import org.picocontainer.defaults.ConstructorInjectionComponentAdapter;
import org.picocontainer.defaults.NotConcreteRegistrationException;

/**
 * @author Alexander Kireyev
 */
public class ExtensionComponentAdapter implements ComponentAdapter, LoadingOrder.Orderable, AssignableToComponentAdapter {
  private Object myComponentInstance;
  private String myImplementationClassName;
  private Element myExtensionElement;
  private PicoContainer myContainer;
  private PluginDescriptor myPluginDescriptor;
  private boolean myDeserializeInstance;
  private ComponentAdapter myDelegate;
  private Class myImplementationClass;

  public ExtensionComponentAdapter(
    String implementationClass,
    Element extensionElement,
    PicoContainer container,
    PluginDescriptor pluginDescriptor,
    boolean deserializeInstance) {
    myImplementationClassName = implementationClass;
    myExtensionElement = extensionElement;
    myContainer = container;
    myPluginDescriptor = pluginDescriptor;
    myDeserializeInstance = deserializeInstance;
  }

  public Object getComponentKey() {
    return this;
  }

  public Class getComponentImplementation() {
    return loadClass(myImplementationClassName);
  }

  public Object getComponentInstance(final PicoContainer container) throws PicoInitializationException, PicoIntrospectionException, AssignabilityRegistrationException, NotConcreteRegistrationException {
    //assert myContainer == container : "Different containers: " + myContainer + " - " + container;

    if (myComponentInstance == null) {
      if (!Element.class.equals(getComponentImplementation())) {
        Object componentInstance = getDelegate().getComponentInstance(container);

        if (myDeserializeInstance) {
          try {
            XmlSerializer.deserializeInto(componentInstance, myExtensionElement);
          }
          catch (Exception e) {
            throw new PicoInitializationException(e);
          }
        }

        myComponentInstance = componentInstance;
      }
      else {
        myComponentInstance = myExtensionElement;
      }
      if (myComponentInstance instanceof PluginAware) {
        PluginAware pluginAware = (PluginAware) myComponentInstance;
        pluginAware.setPluginDescriptor(myPluginDescriptor);
      }
    }

    return myComponentInstance;
  }

  public void verify(PicoContainer container) throws PicoIntrospectionException {
    throw new UnsupportedOperationException("Method verify is not supported in " + getClass());
  }

  public void accept(PicoVisitor visitor) {
    throw new UnsupportedOperationException("Method accept is not supported in " + getClass());
  }

  public Object getExtension() {
    return getComponentInstance(myContainer);
  }

  public LoadingOrder getOrder() {
    String orderAttr = myExtensionElement.getAttributeValue("order");
    return LoadingOrder.readOrder(orderAttr);
  }

  public String getOrderId() {
    return myExtensionElement.getAttributeValue("id");
  }

  private Element getExtensionElement() {
    return myExtensionElement;
  }

  public Element getDescribingElement() {
    return getExtensionElement();
  }

  public PluginId getPluginName() {
    return myPluginDescriptor.getPluginId();
  }

  public PluginDescriptor getPluginDescriptor() {
    return myPluginDescriptor;
  }

  private Class loadClass(final String className) {
    if (myImplementationClass != null) return myImplementationClass;

    try {
      ClassLoader classLoader = myPluginDescriptor != null ? myPluginDescriptor.getPluginClassLoader() : getClass().getClassLoader();
      if (classLoader == null) {
        classLoader = getClass().getClassLoader();
      }


      myImplementationClass = Class.forName(className, true, classLoader);
    }
    catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }

    return myImplementationClass;
  }

  private synchronized ComponentAdapter getDelegate() {
    if (myDelegate == null) {
      myDelegate = new CachingComponentAdapter(new ConstructorInjectionComponentAdapter(getComponentKey(), loadClass(
        myImplementationClassName), null, true));
    }

    return myDelegate;
  }

  public boolean isAssignableTo(Class aClass) {
    return aClass.getName().equals(myImplementationClassName);
  }
}
