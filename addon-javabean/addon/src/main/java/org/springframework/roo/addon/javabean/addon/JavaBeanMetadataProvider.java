package org.springframework.roo.addon.javabean.addon;

import static org.springframework.roo.model.JpaJavaType.TRANSIENT;
import static org.springframework.roo.model.RooJavaType.ROO_JAVA_BEAN;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Provides {@link JavaBeanMetadata}.
 *
 * @author Ben Alex
 * @author Juan Carlos García
 * @author Enrique Ruiz at DISID Corporation S.L.
 * @since 1.0
 */
@Component
@Service
public class JavaBeanMetadataProvider extends AbstractItdMetadataProvider {

  protected final static Logger LOGGER = HandlerUtils.getLogger(JavaBeanMetadataProvider.class);

  private final Set<String> producedMids = new LinkedHashSet<String>();

  private ProjectOperations projectOperations;
  private MemberDetailsScanner memberDetailsScanner;
  private Boolean wasGaeEnabled;

  protected MetadataDependencyRegistryTracker registryTracker = null;

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}</li>
   * <li>Registers {@link RooJavaType#ROO_JAVA_BEAN} as additional JavaType
   * that will trigger metadata registration.</li>
   * </ul>
   */
  @Override
  protected void activate(final ComponentContext cContext) {
    super.activate(cContext);
    BundleContext localContext = cContext.getBundleContext();
    this.registryTracker =
        new MetadataDependencyRegistryTracker(localContext, this,
            PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
    this.registryTracker.open();
    addMetadataTrigger(ROO_JAVA_BEAN);
  }

  /**
   * This service is being deactivated so unregister upstream-downstream
   * dependencies, triggers, matchers and listeners.
   *
   * @param context
   */
  protected void deactivate(final ComponentContext context) {
    MetadataDependencyRegistry registry = this.registryTracker.getService();
    registry.removeNotificationListener(this);
    registry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(),
        getProvidesType());
    this.registryTracker.close();
    removeMetadataTrigger(ROO_JAVA_BEAN);
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return JavaBeanMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType = JavaBeanMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = JavaBeanMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  private JavaSymbolName getIdentifierAccessorMethodName(final FieldMetadata field,
      final String metadataIdentificationString) {

    if (projectOperations == null) {
      projectOperations = getProjectOperations();
    }

    Validate.notNull(projectOperations, "ProjectOperations is required");

    final LogicalPath path = PhysicalTypeIdentifier.getPath(field.getDeclaredByMetadataId());
    final String moduleNme = path.getModule();
    if (projectOperations.isProjectAvailable(moduleNme)
        || !projectOperations.isFeatureInstalled(FeatureNames.GAE)) {
      return null;
    }
    // We are not interested if the field is annotated with
    // @javax.persistence.Transient
    for (final AnnotationMetadata annotationMetadata : field.getAnnotations()) {
      if (annotationMetadata.getAnnotationType().equals(TRANSIENT)) {
        return null;
      }
    }
    JavaType fieldType = field.getFieldType();
    // If the field is a common collection type we need to get the element
    // type
    if (fieldType.isCommonCollectionType()) {
      if (fieldType.getParameters().isEmpty()) {
        return null;
      }
      fieldType = fieldType.getParameters().get(0);
    }

    final MethodMetadata identifierAccessor =
        getPersistenceMemberLocator().getIdentifierAccessor(fieldType);
    if (identifierAccessor != null) {
      getMetadataDependencyRegistry().registerDependency(
          identifierAccessor.getDeclaredByMetadataId(), metadataIdentificationString);
      return identifierAccessor.getMethodName();
    }

    return null;
  }

  public String getItdUniquenessFilenameSuffix() {
    return "JavaBean";
  }

  @Override
  protected ItdTypeDetailsProvidingMetadataItem getMetadata(
      final String metadataIdentificationString, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {
    final JavaBeanAnnotationValues annotationValues =
        new JavaBeanAnnotationValues(governorPhysicalTypeMetadata);
    if (!annotationValues.isAnnotationFound()) {
      return null;
    }

    ClassOrInterfaceTypeDetails currentClassDetails =
        governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();

    final Map<FieldMetadata, JavaSymbolName> declaredFields =
        new LinkedHashMap<FieldMetadata, JavaSymbolName>();
    for (final FieldMetadata field : currentClassDetails.getDeclaredFields()) {
      declaredFields.put(field,
          getIdentifierAccessorMethodName(field, metadataIdentificationString));
    }

    // In order to handle switching between GAE and JPA produced MIDs need
    // to be remembered so they can be regenerated on JPA <-> GAE switch
    producedMids.add(metadataIdentificationString);

    // Getting implements
    List<JavaType> interfaces = currentClassDetails.getImplementsTypes();
    List<? extends MethodMetadata> interfaceMethods = null;
    if (!interfaces.isEmpty()) {
      for (JavaType currentInterface : interfaces) {
        ClassOrInterfaceTypeDetails currentInterfaceDetails =
            getTypeLocationService().getTypeDetails(currentInterface);
        if (currentInterfaceDetails != null) {
          interfaceMethods = currentInterfaceDetails.getDeclaredMethods();
        }
      }
    }

    return new JavaBeanMetadata(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata, annotationValues, declaredFields, interfaceMethods,
        getMemberDetailsScanner());
  }

  public String getProvidesType() {
    return JavaBeanMetadata.getMetadataIdentiferType();
  }

  // We need to notified when ProjectMetadata changes in order to handle JPA
  // <-> GAE persistence changes
  @Override
  protected void notifyForGenericListener(final String upstreamDependency) {

    if (projectOperations == null) {
      projectOperations = getProjectOperations();
    }

    Validate.notNull(projectOperations, "ProjectOperations is required");

    // If the upstream dependency is null or invalid do not continue
    if (StringUtils.isBlank(upstreamDependency)
        || !MetadataIdentificationUtils.isValid(upstreamDependency)) {
      return;
    }
    // If the upstream dependency isn't ProjectMetadata do not continue
    if (!ProjectMetadata.isValid(upstreamDependency)) {
      return;
    }
    // If the project isn't valid do not continue
    if (projectOperations.isProjectAvailable(ProjectMetadata.getModuleName(upstreamDependency))) {
      final boolean isGaeEnabled = projectOperations.isFeatureInstalled(FeatureNames.GAE);
      // We need to determine if the persistence state has changed, we do
      // this by comparing the last known state to the current state
      final boolean hasGaeStateChanged = wasGaeEnabled == null || isGaeEnabled != wasGaeEnabled;
      if (hasGaeStateChanged) {
        wasGaeEnabled = isGaeEnabled;
        for (final String producedMid : producedMids) {
          getMetadataService().evictAndGet(producedMid);
        }
      }
    }
  }

  public ProjectOperations getProjectOperations() {
    return getServiceManager().getServiceInstance(this, ProjectOperations.class);
  }
}
