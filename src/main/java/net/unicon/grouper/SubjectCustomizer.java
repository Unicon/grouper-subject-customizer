package net.unicon.grouper;

import edu.internet2.middleware.grouper.GrouperSession;
import edu.internet2.middleware.grouper.MembershipFinder;
import edu.internet2.middleware.grouper.cfg.GrouperConfig;
import edu.internet2.middleware.grouper.exception.GrouperSessionException;
import edu.internet2.middleware.grouper.membership.MembershipResult;
import edu.internet2.middleware.grouper.misc.GrouperSessionHandler;
import edu.internet2.middleware.grouper.subj.SubjectCustomizerBase;
import edu.internet2.middleware.grouper.util.GrouperUtil;
import edu.internet2.middleware.grouperClient.util.GrouperClientUtils;
import edu.internet2.middleware.subject.Subject;
import edu.internet2.middleware.subject.provider.SubjectImpl;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *  * remove a subjects private information from people who shouldn't see them
 * <p>
 * Specify a group (or expression) who’s member’s attributes should be removed from searches. Folks in the unlessSearcherIsInGroup would still see the attributes, as would the user themselves.
 * Both properties ending in “group” take a group names (etc:ferpaUsers), the expression is a Jexl expression that can evaluate subject’s attributes.
 * subjectCustomizer.redactSubjectAttributes.ifSubjectInGroup.0=
 * subjectCustomizer.redactSubjectAttributes.ifSubjectExpressionIsTrue.0=
 * subjectCustomizer.redactSubjectAttributes.unlessSearcherIsInGroup.0=
 * <p>
 * Specify a group (or expression) who’s member’s should be removed from searches. Folks in the unlessSearcherIsInGroup would still see the user.
 * Both properties ending in “group” take a group names (etc:ferpaUsers), the expression is a Jexl expression that can evaluate subject’s attributes.
 * subjectCustomizer.filterSubjectOut.ifSubjectInGroup.0=
 * subjectCustomizer.filterSubjectOut.ifSubjectExpressionIsTrue.0=
 * subjectCustomizer.filterSubjectOut.unlessSearcherIsInGroup.0=
 * <p>
 *
 *  * @author jgasper
 *  
 */
public class SubjectCustomizer extends SubjectCustomizerBase {

    private final static Logger logger = LoggerFactory.getLogger(SubjectCustomizer.class);
    /**
     * attribute name to use for the users display name
     */
    public static final String UID_ATTRIBUTE = GrouperConfig.retrieveConfig().propertyValueString("subjectCustomizer.uidField", "uid");

    /**
     * attribute name to show "(restricted)"
     */
    public static final String RESTRICTED_ATTRIBUTE_NAME = GrouperConfig.retrieveConfig().propertyValueString("subjectCustomizer.restrictedAttributeName", "cn");

    /**
     * attribute value to use instead of "(restricted)"
     */
    public static final String RESTRICTED_ATTRIBUTE_VALUE = GrouperConfig.retrieveConfig().propertyValueString("subjectCustomizer.restrictedAttributeValue", "(restricted)");


    /**
     *  * @see SubjectCustomizer#filterSubjects(GrouperSession, Set, String)
     *  
     */
    @Override
    public Set<Subject> filterSubjects(final GrouperSession grouperSession, final Set<Subject> subjects, final String findSubjectsInStemName) {

        //nothing to do if no results
        if (GrouperUtil.length(subjects) == 0) {
            return subjects;
        } else {
            logger.debug("starting with {} subjects", subjects.size());
        }

        final Subject searcher = grouperSession.getSubject();

        final Set<Subject> revisedSubjectsList = new HashSet<Subject>();

        try {
            GrouperSession.callbackGrouperSession(
                    GrouperSession.staticGrouperSession().internal_getRootSession(), new GrouperSessionHandler() {
                        @Override
                        public Object callback(final GrouperSession grouperSession) throws GrouperSessionException {

                            /**
                             * Build a MembershipResult that includes a candidate list of privileged groups that the searcher or subjects maybe
                             * a member of. it still needs to be determined if the subject qualifies for filtering/redacting
                             * Getting results in one query
                             */
                            MembershipFinder membershipFinder = new MembershipFinder()
                                    .assignCheckSecurity(false)
                                    .addSubject(searcher)
                                    .addSubjects(subjects);

                            logger.debug("Collecting all of the involved groups for each set: filtered");
                            for (int i = 0; i > -1; i++) {
                                final String filterSubjectOutIfSubjectInGroup = GrouperConfig.retrieveConfig().propertyValueString(String.format("subjectCustomizer.filterSubjectOut.ifSubjectInGroup.%d", i), "");
                                final String filterSubjectOutUnlessSearcherIsInGroup = GrouperConfig.retrieveConfig().propertyValueString(String.format("subjectCustomizer.filterSubjectOut.unlessSearcherIsInGroup.%d", i), "");

                                if (StringUtils.isEmpty(filterSubjectOutIfSubjectInGroup) && StringUtils.isEmpty(filterSubjectOutUnlessSearcherIsInGroup)) {
                                    break;
                                }

                                if (StringUtils.isNotEmpty(filterSubjectOutIfSubjectInGroup)) {
                                    logger.debug("adding {} to the query", filterSubjectOutIfSubjectInGroup);
                                    membershipFinder.addGroup(filterSubjectOutIfSubjectInGroup);
                                }

                                if (StringUtils.isNotEmpty(filterSubjectOutUnlessSearcherIsInGroup)) {
                                    logger.debug("adding {} to the query", filterSubjectOutUnlessSearcherIsInGroup);
                                    membershipFinder.addGroup(filterSubjectOutUnlessSearcherIsInGroup);
                                }
                            }

                            logger.debug("Collect all of the involved groups for each set: redacted");
                            for (int i = 0; i > -1; i++) {
                                final String redactSubjectAttributesIfSubjectInGroup = GrouperConfig.retrieveConfig().propertyValueString(String.format("subjectCustomizer.redactSubjectAttributes.ifSubjectInGroup.%d", i), "");
                                final String redactSubjectAttributesUnlessSearcherIsInGroup = GrouperConfig.retrieveConfig().propertyValueString(String.format("subjectCustomizer.redactSubjectAttributes.unlessSearcherIsInGroup.%d", i), "");

                                if (StringUtils.isEmpty(redactSubjectAttributesIfSubjectInGroup) && StringUtils.isEmpty(redactSubjectAttributesUnlessSearcherIsInGroup)) {
                                    break;
                                }

                                if (StringUtils.isNotEmpty(redactSubjectAttributesIfSubjectInGroup)) {
                                    logger.debug("adding {} to the query", redactSubjectAttributesIfSubjectInGroup);
                                    membershipFinder.addGroup(redactSubjectAttributesIfSubjectInGroup);
                                }

                                if (StringUtils.isNotEmpty(redactSubjectAttributesUnlessSearcherIsInGroup)) {
                                    logger.debug("adding {} to the query", redactSubjectAttributesUnlessSearcherIsInGroup);
                                    membershipFinder.addGroup(redactSubjectAttributesUnlessSearcherIsInGroup);
                                }
                            }

                            logger.debug("querying all of the groups and subjects and search subject");
                            final MembershipResult groupMembershipResult = membershipFinder
                                    .findMembershipResult();


                            for (Subject subject : subjects) {
                                logger.debug("processing {} ({})", subject.getId(), subject.getSourceId());

                                if (subject.getSourceId().equalsIgnoreCase(searcher.getSourceId()) && subject.getId().equalsIgnoreCase(searcher.getId())) {
                                    logger.debug("searcher is also the subject... not filtering or redacting");
                                    revisedSubjectsList.add(subject);

                                } else if (!shouldFilterOut(subject, searcher, groupMembershipResult, grouperSession)) {

                                    revisedSubjectsList.add(redactSubjectIfNecessary(subject, searcher, groupMembershipResult, grouperSession));
                                } else {
                                    logger.debug("filtered out {} ({})", subject.getId(), subject.getSourceId());
                                }
                            }
                            return null;
                        }

                    });
        } catch (final Exception e) {
            logger.error("something bad happened: {}", e.getMessage(), e);
        }

        logger.debug("ended with {} subjects", revisedSubjectsList.size());
        return revisedSubjectsList;
    }


    /**
     * Determines if the subject should be filtered out from the search results by checking if the subject meets one of the filter
     * criteria, and the searcher isn't in the associated privilege group (found in the membershipResults.
     *
     * @param subject          a result of a standard subject search.
     * @param searcher         the subject doing the search
     * @param membershipResult a membership search result of the priv'd groups that the search is a member of.
     * @return a flag indicating whether to filter (remove) the subject or not.
     */
    protected boolean shouldFilterOut(Subject subject, Subject searcher, MembershipResult membershipResult, GrouperSession grouperSession) {
        Boolean applyFilter = null;

        Map<String, Object> map = new HashMap<>();
        map.put("subject", subject);

        for (int i = 0; i > -1; i++) {
            final String filterSubjectOutIfSubjectInGroup = GrouperConfig.retrieveConfig().propertyValueString(String.format("subjectCustomizer.filterSubjectOut.ifSubjectInGroup.%d", i), "");
            final String filterSubjectOutIfSubjectExpression = GrouperConfig.retrieveConfig().propertyValueString(String.format("subjectCustomizer.filterSubjectOut.ifSubjectExpressionIsTrue.%d", i), "");
            final String filterSubjectOutUnlessSearcherIsInGroup = GrouperConfig.retrieveConfig().propertyValueString(String.format("subjectCustomizer.filterSubjectOut.unlessSearcherIsInGroup.%d", i), "");

            if (StringUtils.isEmpty(filterSubjectOutIfSubjectInGroup) && StringUtils.isEmpty(filterSubjectOutUnlessSearcherIsInGroup)) {
                break;
            }

            if (membershipResult.hasGroupMembership(filterSubjectOutUnlessSearcherIsInGroup, searcher)) {
                //the searcher is privileged in some rule so they will get this record.
                logger.debug("explicitly not filtering as the searcher is a member of {}", filterSubjectOutUnlessSearcherIsInGroup);
                applyFilter = false;
                break;

            } else {
                if ((StringUtils.isNotEmpty(filterSubjectOutIfSubjectInGroup) && membershipResult.hasGroupMembership(filterSubjectOutIfSubjectInGroup, subject)) ||
                        (StringUtils.isNotEmpty(filterSubjectOutIfSubjectExpression)) && Boolean.parseBoolean(GrouperClientUtils.substituteExpressionLanguage(filterSubjectOutIfSubjectExpression, map, true, false, true, true))) {

                    logger.debug("{} ({}) should be filtered out by rule number {}", new String[] {subject.getId(), subject.getSourceId(), String.valueOf(i)});
                    applyFilter = true;
                }
            }
        }

        return applyFilter == null ? false : applyFilter;
    }

    /**
     * Determines if the subject should be filtered out from the search results by checking if the subject meets one of the filter
     * criteria, and the searcher isn't in the associated privilege group (found in the membershipResults.
     *
     * @param subject          a result of a standard subject search.
     * @param searcher         the subject doing the search
     * @param membershipResult a membership search result of the priv'd groups that the search is a member of.
     * @return a flag indicating whether to filter (remove) the subject or not.
     */
    protected Subject redactSubjectIfNecessary(Subject subject, Subject searcher, MembershipResult membershipResult, GrouperSession grouperSession) {
        Boolean applyRedaction = null;

        Map<String, Object> map = new HashMap<>();
        map.put("subject", subject);

        for (int i = 0; i > -1; i++) {
            final String redactSubjectAttributesIfSubjectInGroup = GrouperConfig.retrieveConfig().propertyValueString(String.format("subjectCustomizer.redactSubjectAttributes.ifSubjectInGroup.%d", i), "");
            final String redactSubjectAttributesIfSubjectExpressionIsTrue = GrouperConfig.retrieveConfig().propertyValueString(String.format("subjectCustomizer.redactSubjectAttributes.ifSubjectExpressionIsTrue.%d", i), "");
            final String redactSubjectAttributesUnlessSearcherIsInGroup = GrouperConfig.retrieveConfig().propertyValueString(String.format("subjectCustomizer.redactSubjectAttributes.unlessSearcherIsInGroup.%d", i), "");


            if (StringUtils.isEmpty(redactSubjectAttributesIfSubjectInGroup) && StringUtils.isEmpty(redactSubjectAttributesIfSubjectExpressionIsTrue)) {
                break;
            }

            if (membershipResult.hasGroupMembership(redactSubjectAttributesUnlessSearcherIsInGroup, searcher)) {
                logger.debug("explicitly not filtering as the searcher is a member of {}", redactSubjectAttributesUnlessSearcherIsInGroup);
                applyRedaction = false;
                break;

            } else {
                if ((StringUtils.isNotEmpty(redactSubjectAttributesIfSubjectInGroup) && membershipResult.hasGroupMembership(redactSubjectAttributesIfSubjectInGroup, subject)) ||
                        (StringUtils.isNotEmpty(redactSubjectAttributesIfSubjectExpressionIsTrue)) && Boolean.parseBoolean(GrouperClientUtils.substituteExpressionLanguage(redactSubjectAttributesIfSubjectExpressionIsTrue, map, true, false, true, true))) {
                    logger.debug("{} ({}) should be redacted by rule number {}", new String[] {subject.getId(), subject.getSourceId(), String.valueOf(i)});
                    applyRedaction = true;
                }
            }

        }

        if (applyRedaction == null || applyRedaction == false) {
            logger.debug("returning un-redacted {} ({})", subject.getId(), subject.getSourceId());
            return subject;
        }

        final String netId = subject.getAttributeValue(UID_ATTRIBUTE, false);
        final Subject replacementSubject = new SubjectImpl(subject.getId(), netId, "", subject.getTypeName(), subject.getSourceId());
        replacementSubject.getAttributes(false).put(RESTRICTED_ATTRIBUTE_NAME, GrouperUtil.toSet(RESTRICTED_ATTRIBUTE_VALUE));

        logger.debug("returning redacted {} ({})", subject.getId(), subject.getSourceId());
        return replacementSubject;
    }

}
