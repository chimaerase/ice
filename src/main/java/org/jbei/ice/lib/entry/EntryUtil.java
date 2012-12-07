package org.jbei.ice.lib.entry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jbei.ice.controllers.ApplicationController;
import org.jbei.ice.controllers.common.ControllerException;
import org.jbei.ice.lib.account.AccountController;
import org.jbei.ice.lib.account.model.Account;
import org.jbei.ice.lib.entry.model.Entry;
import org.jbei.ice.lib.entry.model.EntryFundingSource;
import org.jbei.ice.lib.entry.model.PartNumber;
import org.jbei.ice.lib.entry.model.Plasmid;
import org.jbei.ice.lib.entry.model.Strain;
import org.jbei.ice.lib.logging.Logger;
import org.jbei.ice.lib.permissions.PermissionException;
import org.jbei.ice.lib.utils.Utils;
import org.jbei.ice.shared.dto.ConfigurationKey;

import com.google.common.base.Joiner;

/**
 * Utility class for operating on entries
 *
 * @author Hector Plahar
 */
public class EntryUtil {

    /**
     * Generate the comma separated string representation of {@link org.jbei.ice.lib.entry.model.PartNumber}s
     * associated with
     * this entry.
     *
     * @return Comma separated part numbers.
     */
    public static String getPartNumbersAsString(Entry entry) {
        Joiner joiner = Joiner.on(", ").skipNulls();
        return joiner.join(entry.getPartNumbers());
    }

    /**
     * Generate a clickable &lt;a&gt; link from the given {@link IceLink}.
     * <p/>
     * If the partnumber referred in the link does not exist, it creates a non-clickable text.
     *
     * @param iceLink link to create.
     * @return Html of the link.
     */
    private static String makeEntryLink(Account account, IceLink iceLink) throws ControllerException {
        String result;

        EntryController entryController = ApplicationController.getEntryController();

        long id = 0;
        Entry entry;

        try {
            entry = entryController.getByPartNumber(account, iceLink.getPartNumber());
        } catch (PermissionException e) {
            throw new ControllerException(e);
        }

        if (entry != null) {
            id = entry.getId();
        }

        String descriptiveLabel;
        if (iceLink.getDescriptiveLabel() == null) {
            descriptiveLabel = iceLink.getPartNumber();
        } else if (iceLink.getDescriptiveLabel().equals("")) {
            descriptiveLabel = iceLink.getPartNumber();
        } else {
            descriptiveLabel = iceLink.getDescriptiveLabel();
        }

        result = "<a href=/entry/view/" + id + ">" + descriptiveLabel + "</a>";
        return result;
    }

    public static String principalInvestigatorToString(Set<EntryFundingSource> fundingSources) {
        if (fundingSources == null || fundingSources.isEmpty())
            return "";

        String str = null;
        for (EntryFundingSource source : fundingSources) {
            if (str != null)
                str += (", " + source.getFundingSource().getPrincipalInvestigator());
            else
                str = source.getFundingSource().getPrincipalInvestigator();
        }

        return str;
    }

    public static String fundingSourceToString(Set<EntryFundingSource> fundingSources) {
        if (fundingSources == null || fundingSources.isEmpty())
            return "";

        String str = null;
        for (EntryFundingSource source : fundingSources) {
            if (str != null)
                str += (", " + source.getFundingSource().getFundingSource());
            else
                str = source.getFundingSource().getFundingSource();
        }

        return str;
    }

    /**
     * Generate an html &lt;a&gt; link from the given url.
     *
     * @param text Url to linkify.
     * @return Html &lt;a&gt; link.
     */
    public static String urlLinkifyText(String text) {

        Pattern urlPattern = Pattern.compile("\\bhttp://(\\S*)\\b");
        Pattern secureUrlPattern = Pattern.compile("\\bhttps://(\\S*)\\b");

        if (text == null) {
            return "";
        }

        class UrlLinkText {
            private String url = "";
            private int start = 0;
            private int end = 0;

            public UrlLinkText(String url, int start, int end) {
                setUrl(url);
                setStart(start);
                setEnd(end);
            }

            public String getUrl() {
                return url;
            }

            public void setUrl(String url) {
                this.url = url;
            }

            public int getStart() {
                return start;
            }

            public void setStart(int start) {
                this.start = start;
            }

            public int getEnd() {
                return end;
            }

            public void setEnd(int end) {
                this.end = end;
            }
        }

        class UrlComparator implements Comparator<UrlLinkText> {
            @Override
            public int compare(UrlLinkText o1, UrlLinkText o2) {
                if (o1.getStart() < o2.getStart()) {
                    return -1;
                } else if (o1.getStart() > o2.getStart()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        }
        UrlComparator urlComparator = new UrlComparator();

        ArrayList<UrlLinkText> urls = new ArrayList<>();
        Matcher urlMatcher = urlPattern.matcher(text);
        while (urlMatcher.find()) {
            urls.add(new UrlLinkText(urlMatcher.group(0).trim(), urlMatcher.start(), urlMatcher
                    .end()));
        }
        Matcher secureUrlMatcher = secureUrlPattern.matcher(text);
        while (secureUrlMatcher.find()) {
            urls.add(new UrlLinkText(secureUrlMatcher.group(0).trim(), secureUrlMatcher.start(),
                                     secureUrlMatcher.end()));
        }
        Collections.sort(urls, urlComparator);
        String newText = text;
        for (int i = urls.size() - 1; i > -1; i = i - 1) {
            String before = newText.substring(0, urls.get(i).getStart());
            String after = newText.substring(urls.get(i).getEnd());
            newText = before + "<a href=" + urls.get(i).getUrl() + ">" + urls.get(i).getUrl() + "</a>" + after;
        }

        return newText;
    }

    public static String linkifyText(Account account, String text) {
        String newText = wikiLinkifyText(account, text);
        newText = urlLinkifyText(newText);
        return newText;
    }

    /**
     * Generate an html &lt;a&gt; link from the given {@link IceLink} text.
     *
     * @param text IceLink text.
     * @return Html &lt;a&gt; link.
     */
    private static String wikiLinkifyText(Account account, String text) {
        String newText;

        try {
            EntryController entryController = ApplicationController.getEntryController();
            String wikiLink = Utils.getConfigValue(ConfigurationKey.WIKILINK_PREFIX);

            Pattern basicWikiLinkPattern = Pattern.compile("\\[\\[" + wikiLink + ":.*?\\]\\]");
            Pattern partNumberPattern = Pattern.compile("\\[\\[" + wikiLink + ":(.*)\\]\\]");
            Pattern descriptivePattern = Pattern.compile("\\[\\[" + wikiLink + ":(.*)\\|(.*)\\]\\]");

            if (text == null) {
                return "";
            }
            Matcher basicWikiLinkMatcher = basicWikiLinkPattern.matcher(text);

            ArrayList<IceLink> jbeiLinks = new ArrayList<IceLink>();
            ArrayList<Integer> starts = new ArrayList<Integer>();
            ArrayList<Integer> ends = new ArrayList<Integer>();

            while (basicWikiLinkMatcher.find()) {
                String partNumber = null;
                String descriptive = null;

                Matcher partNumberMatcher = partNumberPattern.matcher(basicWikiLinkMatcher.group());
                Matcher descriptivePatternMatcher = descriptivePattern.matcher(basicWikiLinkMatcher.group());

                if (descriptivePatternMatcher.find()) {
                    partNumber = descriptivePatternMatcher.group(1).trim();
                    descriptive = descriptivePatternMatcher.group(2).trim();

                } else if (partNumberMatcher.find()) {
                    partNumber = partNumberMatcher.group(1).trim();
                }

                if (partNumber != null) {
                    try {
                        Entry entry = entryController.getByPartNumber(account, partNumber);

                        if (entry != null) {
                            jbeiLinks.add(new IceLink(partNumber, descriptive));
                            starts.add(basicWikiLinkMatcher.start());
                            ends.add(basicWikiLinkMatcher.end());
                        }
                    } catch (PermissionException pe) {
                        Logger.warn(account.getEmail() + ": No permissions for part_number " + partNumber);
                        return text;
                    }
                }
            }

            newText = new String(text);
            for (int i = jbeiLinks.size() - 1; i > -1; i = i - 1) {
                String before = newText.substring(0, starts.get(i));
                String after = newText.substring(ends.get(i));
                newText = before + makeEntryLink(account, jbeiLinks.get(i)) + after;
            }
        } catch (Exception e) {
            Logger.error(e);
            return text;
        }

        return newText;
    }

    public static String getParsedNotes(String s) {
        if (s == null) {
            return null;
        }

        final StringBuilder buffer = new StringBuilder();
        int newlineCount = 0;

        buffer.append("<p>");
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);

            switch (c) {
                case '\n':
                    newlineCount++;
                    break;

                case '\r':
                    break;

                default:
                    if (newlineCount == 1) {
                        buffer.append("<br/>");
                    } else if (newlineCount > 1) {
                        buffer.append("</p><p>");
                    }

                    buffer.append(c);
                    newlineCount = 0;
                    break;
            }
        }
        if (newlineCount == 1) {
            buffer.append("<br/>");
        } else if (newlineCount > 1) {
            buffer.append("</p><p>");
        }
        buffer.append("</p>");
        return buffer.toString();
    }

    /**
     * Hold information about the ICE link.
     * <p/>
     * These links are modeled after wikipedia/mediawiki links. They are of the form
     * <p/>
     * [[jbei:JBx_000001]] or [[jbei:JBx_000001 | Descriptive label]] format, just like mediawiki
     * links.
     * <p/>
     * The prefix (for example "jbei") can be changed in the preferences file.
     *
     * @author Timothy Ham
     */
    public static class IceLink {
        private String descriptiveLabel = "";
        private String partNumber = "";

        /**
         * Contructor.
         *
         * @param partNumber       Part number.
         * @param descriptiveLabel Descriptive label.
         */
        public IceLink(String partNumber, String descriptiveLabel) {
            this.partNumber = partNumber;
            this.descriptiveLabel = descriptiveLabel;
        }

        /**
         * Get the descriptive label.
         *
         * @return descriptive label.
         */
        public String getDescriptiveLabel() {
            return descriptiveLabel;
        }

        /**
         * Get the part number string.
         *
         * @return Part number.
         */
        public String getPartNumber() {
            return partNumber;
        }
    }

    /**
     * Retrieve the {@link org.jbei.ice.lib.entry.model.Strain} objects associated with the given {@link
     * org.jbei.ice.lib.entry.model.Plasmid}.
     * <p/>
     * Strain objects have a field "plasmids", which is maybe wiki text of plasmids that the strain
     * may harbor. However, since plasmids can be harbored in multiple strains, the reverse lookup
     * must be computed in order to find which strains harbor a plasmid. And since it is possible to
     * import/export strains separately from plasmids, it is possible that even though the strain
     * claims to have a plasmid, that plasmid may not be in this system, but some other. So, in
     * order to find out which strains actually harbor the given plasmid, we must query the strains
     * table for the plasmid, parse the wiki text, and check that those plasmids actually exist
     * before being certain that strain actually harbors this plasmid.
     *
     * @param plasmid
     * @return LinkedHashSet of Strain objects.
     */
    public static LinkedHashSet<Strain> getStrainsForPlasmid(Plasmid plasmid) {
        LinkedHashSet<Strain> resultStrains = new LinkedHashSet<Strain>();
        EntryController entryController = ApplicationController.getEntryController();
        String wikiLink = Utils.getConfigValue(ConfigurationKey.WIKILINK_PREFIX);

        Pattern basicWikiLinkPattern = Pattern.compile("\\[\\[" + wikiLink + ":.*?\\]\\]");
        Pattern partNumberPattern = Pattern.compile("\\[\\[" + wikiLink + ":(.*)\\]\\]");
        Pattern descriptivePattern = Pattern.compile("\\[\\[" + wikiLink + ":(.*)\\|(.*)\\]\\]");

        AccountController accountController = ApplicationController.getAccountController();
        HashSet<Long> strainIds;

        Account account;
        try {
            // TODO : temp measure till utils manager is also converted
            strainIds = entryController.retrieveStrainsForPlasmid(plasmid);
            account = accountController.getSystemAccount();
            if (strainIds == null)
                return null;

            for (long strainId : strainIds) {
                Strain strain = (Strain) entryController.get(account, strainId);

                String[] strainPlasmids = strain.getPlasmids().split(",");
                for (String strainPlasmid : strainPlasmids) {
                    strainPlasmid = strainPlasmid.trim();
                    Matcher basicWikiLinkMatcher = basicWikiLinkPattern.matcher(strainPlasmid);
                    String strainPlasmidNumber = null;
                    if (basicWikiLinkMatcher.matches()) {
                        Matcher partNumberMatcher = partNumberPattern.matcher(basicWikiLinkMatcher.group());
                        Matcher descriptivePatternMatcher = descriptivePattern.matcher(basicWikiLinkMatcher.group());

                        if (descriptivePatternMatcher.find()) {
                            strainPlasmidNumber = descriptivePatternMatcher.group(1).trim();
                        } else if (partNumberMatcher.find()) {
                            strainPlasmidNumber = partNumberMatcher.group(1).trim();
                        }

                        if (strainPlasmidNumber != null) {
                            for (PartNumber plasmidPartNumber : plasmid.getPartNumbers()) {
                                if (plasmidPartNumber.getPartNumber().equals(strainPlasmidNumber)) {
                                    resultStrains.add(strain);
                                    break;
                                }
                            }
                        }

                    } else {
                        if (plasmid.getPartNumbers().contains(strainPlasmid)) {
                            resultStrains.add(strain);
                        }
                    }
                }
            }
        } catch (ControllerException e) {
            Logger.error(e);
            return null;
        } catch (PermissionException e) {
            Logger.warn(e.getMessage());
            return null;
        }

        return resultStrains;
    }
}
