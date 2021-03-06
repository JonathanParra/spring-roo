package org.springframework.roo.addon.creator;

import static org.springframework.roo.shell.OptionContexts.UPDATE;

import java.io.File;
import java.util.Locale;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

/**
 * Commands for the 'addon create' add-on to be used by the ROO shell.
 *
 * @author Stefan Schmidt
 * @author Juan Carlos García
 * @since 1.1
 */
@Component
@Service
public class CreatorCommands implements CommandMarker {

  @Reference
  private CreatorOperations creatorOperations;

  @CliAvailabilityIndicator({"addon create i18n", "addon create simple", "addon create advanced",
      "addon create wrapper", "addon create suite"})
  public boolean isCreateAddonAvailable() {
    return creatorOperations.isAddonCreatePossible();
  }

  @CliCommand(value = "addon create simple",
      help = "Create a new simple add-on for Spring Roo (commands + operations).")
  public void simple(
      @CliOption(key = "topLevelPackage", mandatory = true, optionContext = UPDATE,
          help = "The top level package of the new addon. In Maven, this will be the `<groupId>`.") final JavaPackage tlp,
      @CliOption(key = "description", mandatory = false,
          help = "Description of your addon (surround text with double quotes).") final String description,
      @CliOption(
          key = "projectName",
          mandatory = false,
          help = "Provide a custom project name. In Maven, this will be the `<artifactId>`."
              + "Default if option not present: the top level package specified in `--topLevelPackage`.") final String projectName) {

    creatorOperations.createSimpleAddon(tlp, description, projectName, null);
  }

  @CliCommand(value = "addon create advanced",
      help = "Create a new advanced add-on for Spring Roo (commands + operations + "
          + "metadata + trigger annotation + dependencies).")
  public void advanced(
      @CliOption(key = "topLevelPackage", mandatory = true, optionContext = UPDATE,
          help = "The top level package of the new addon. In Maven, this will be the `<groupId>`.") final JavaPackage tlp,
      @CliOption(key = "description", mandatory = false,
          help = "Description of your addon (surround text with double quotes)") final String description,
      @CliOption(
          key = "projectName",
          mandatory = false,
          help = "Provide a custom project name. In Maven, this will be the `<artifactId>`."
              + "Default if option not present: the top level package specified in `--topLevelPackage`.") final String projectName) {

    creatorOperations.createAdvancedAddon(tlp, description, projectName, null);
  }

  @CliCommand(
      value = "addon create suite",
      help = "Create a new Spring Roo Addon Suite for Spring Roo (two sample addons + repository + "
          + "suite generator).")
  public void suite(
      @CliOption(key = "topLevelPackage", mandatory = true, optionContext = UPDATE,
          help = "The top level package of all Spring Roo Addon Suite. In Maven, this will be the "
              + "`<groupId>`.") final JavaPackage tlp,
      @CliOption(key = "description", mandatory = false,
          help = "Description of your Roo Addon Suite (surround text with double quotes)") final String description,
      @CliOption(
          key = "projectName",
          mandatory = false,
          help = "Provide a custom project name for root module. In Maven, this will be the `<artifactId>`."
              + "Default if option not present: the top level package specified in `--topLevelPackage`.") final String projectName) {
    creatorOperations.createRooAddonSuite(tlp, description, projectName);
  }

  @CliCommand(
      value = "addon create i18n",
      help = "Create a new Internationalization add-on for Spring Roo, with a new language. Created "
          + "add-on can be installed later into a project for localizing the project to that new language.")
  public void i18n(
      @CliOption(key = "topLevelPackage", mandatory = true, optionContext = UPDATE,
          help = "The top level package of all Spring Roo Addon Suite. In Maven, this will be the "
              + "`<groupId>`.") final JavaPackage tlp,
      @CliOption(
          key = "locale",
          mandatory = true,
          help = "The locale abbreviation (ie: en, or more specific like en_AU, or de_DE).) for the "
              + "new language.") final Locale locale,
      @CliOption(
          key = "messageBundle",
          mandatory = true,
          help = "Fully qualified path to the messages_xx.properties file which contains the messages in"
              + " the new language.") final File messageBundle,
      @CliOption(key = "language", mandatory = false,
          help = "The full name of the language (used as a label for the UI).") final String language,
      @CliOption(key = "flagGraphic", mandatory = false,
          help = "Fully qualified path to new flag xx.png file.") final File flagGraphic,
      @CliOption(key = "description", mandatory = false,
          help = "Description of your addon (surround text with double quotes).") final String description,
      @CliOption(
          key = "projectName",
          mandatory = false,
          help = "Provide a custom project name. In Maven, this will be the `<artifactId>`."
              + "Default if option not present: the top level package specified in `--topLevelPackage`.") final String projectName) {

    if (locale == null) {
      throw new IllegalStateException(
          "Could not read provided locale. Please use correct format (ie: en, or more specific like en_AU, or de_DE)");
    }
    creatorOperations.createI18nAddon(tlp, language, locale, messageBundle, flagGraphic,
        description, projectName);
  }

  @CliCommand(value = "addon create wrapper",
      help = "Create a new add-on for Spring Roo which wraps a maven artifact to create a OSGi "
          + "compliant bundle")
  public void wrapper(
      @CliOption(key = "topLevelPackage", mandatory = true, optionContext = UPDATE,
          help = "The top level package of the new wrapper bundle.") final JavaPackage tlp,
      @CliOption(key = "groupId", mandatory = true, help = "Dependency group id.") final String groupId,
      @CliOption(key = "artifactId", mandatory = true, help = "Dependency artifact id.") final String artifactId,
      @CliOption(key = "version", mandatory = true, help = "Dependency version.") final String version,
      @CliOption(key = "vendorName", mandatory = true, help = "Dependency vendor name.") final String vendorName,
      @CliOption(key = "licenseUrl", mandatory = true, help = "Dependency license URL.") final String lincenseUrl,
      @CliOption(key = "docUrl", mandatory = false, help = "Dependency documentation URL") final String docUrl,
      @CliOption(
          key = "description",
          mandatory = false,
          help = "Description of the bundle (use keywords with #-tags for better search integration).") final String description,
      @CliOption(
          key = "projectName",
          mandatory = false,
          help = "Provide a custom project name. In Maven, this will be the `<artifactId>`."
              + "Default if option not present: the top level package specified in `--topLevelPackage`.") final String projectName,
      @CliOption(key = "osgiImports", mandatory = false,
          help = "Contents of Import-Package in OSGi manifest.") final String osgiImports) {

    creatorOperations.createWrapperAddon(tlp, groupId, artifactId, version, vendorName,
        lincenseUrl, docUrl, osgiImports, description, projectName);
  }
}
