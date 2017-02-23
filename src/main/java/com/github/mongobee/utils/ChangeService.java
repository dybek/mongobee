package com.github.mongobee.utils;

import com.github.mongobee.changeset.ChangeEntry;
import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.changeset.ChangeSet;
import org.reflections.Reflections;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.core.type.StandardMethodMetadata;
import org.springframework.util.MultiValueMap;

import java.lang.reflect.Method;
import java.util.*;

import static java.util.Arrays.asList;

/**
 * Utilities to deal with reflections and annotations
 *
 * @author lstolowski
 * @since 27/07/2014
 */
public class ChangeService {

  private final String changeLogsBasePackage;
  private final Environment environment;

  public ChangeService(String changeLogsBasePackage) {
    this(changeLogsBasePackage, null);
  }

  public ChangeService(String changeLogsBasePackage, Environment environment) {
    this.changeLogsBasePackage = changeLogsBasePackage;
    this.environment = environment;
  }

  public List<Class<?>> fetchChangeLogs(){
    Reflections reflections = new Reflections(changeLogsBasePackage);
    Set<Class<?>> changeLogs = reflections.getTypesAnnotatedWith(ChangeLog.class); // TODO remove dependency, do own method
    List<Class<?>> filteredChangeLogs = filterClassesByActiveProfiles(changeLogs);

    Collections.sort(filteredChangeLogs, new ChangeLogComparator());

    return filteredChangeLogs;
  }

  public List<Method> fetchChangeSets(final Class<?> type) {
    final List<Method> changeSets = filterChangeSetAnnotation(asList(type.getDeclaredMethods()));
    final List<Method> filteredChangeSets = filterMethodsByActiveProfiles(changeSets);

    Collections.sort(filteredChangeSets, new ChangeSetComparator());

    return filteredChangeSets;
  }

  public boolean isRunAlwaysChangeSet(Method changesetMethod){
    if (changesetMethod.isAnnotationPresent(ChangeSet.class)){
      ChangeSet annotation = changesetMethod.getAnnotation(ChangeSet.class);
      return annotation.runAlways();
    } else {
      return false;
    }
  }

  public ChangeEntry createChangeEntry(Method changesetMethod){
    if (changesetMethod.isAnnotationPresent(ChangeSet.class)){
      ChangeSet annotation = changesetMethod.getAnnotation(ChangeSet.class);
  
      return new ChangeEntry(
          annotation.id(),
          annotation.author(),
          new Date(),
          changesetMethod.getDeclaringClass().getName(),
          changesetMethod.getName());
    } else {
      return null;
    }
  }
  private boolean matchesActiveSpringProfile(Environment environment, AnnotatedTypeMetadata metadata) {
    if (environment != null) {
      MultiValueMap<String, Object> attrs = metadata.getAllAnnotationAttributes(Profile.class.getName());
      if (attrs != null) {
        for (Object value : attrs.get("value")) {
          if (environment.acceptsProfiles(((String[]) value))) {
            return true;
          }
        }
        return false;
      }
    }
    return true;
  }

  private List<Method> filterMethodsByActiveProfiles(Collection<? extends Method> annotated) {
    List<Method> filtered = new ArrayList<>();
    for (Method element : annotated) {
      AnnotatedTypeMetadata metadata = new StandardMethodMetadata(element, true);
      if (matchesActiveSpringProfile(environment, metadata)){
        filtered.add( element);
      }
    }
    return filtered;
  }

  private List<Class<?>> filterClassesByActiveProfiles(Collection<? extends Class> annotated) {
    List<Class<?>> filtered = new ArrayList<>();
    for (Class<?> element : annotated) {
      AnnotatedTypeMetadata metadata = new StandardAnnotationMetadata(element, true);
      if (matchesActiveSpringProfile(environment, metadata)){
        filtered.add(element);
      }
    }
    return filtered;
  }

  private List<Method> filterChangeSetAnnotation(List<Method> allMethods) {
    final List<Method> changesetMethods = new ArrayList<>();
    for (final Method method : allMethods) {
      if (method.isAnnotationPresent(ChangeSet.class)) {
        changesetMethods.add(method);
      }
    }
    return changesetMethods;
  }

}
