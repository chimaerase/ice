package org.jbei.ice.lib.models;

import org.hibernate.annotations.Type;
import org.jbei.ice.lib.dao.IDataModel;
import org.jbei.ice.lib.vo.DNAFeature;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlTransient;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Stores the sequence annotation information, and associates {@link Feature} objects to a
 * {@link Sequence} object.
 * <p/>
 * SequenceFeature represents is a many-to-many mapping. In addition, this class has fields to store
 * sequence specific annotation information.
 *
 * @author Timothy Ham, Zinovii Dmytriv
 */
@Entity
@Table(name = "sequence_feature")
@SequenceGenerator(name = "sequence", sequenceName = "sequence_feature_id_seq", allocationSize = 1)
public class SequenceFeature implements IDataModel {

    public static final String DESCRIPTION = "description";

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "sequence")
    private long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sequence_id")
    private Sequence sequence;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "feature_id")
    private Feature feature;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "sequenceFeature", orphanRemoval = true)
    private final Set<AnnotationLocation> annotationLocations = new LinkedHashSet<>();

    @Deprecated
    @Column(name = "feature_start")
    private int genbankStart;

    @Deprecated
    @Column(name = "feature_end")
    private int end;

    /**
     * +1 or -1
     */
    @Column(name = "strand")
    private int strand;

    @Column(name = "name", length = 127)
    private String name;

    /**
     * Deprecated since schema 0.8.0. Use SequenceFeatureAttribute with "description" as key
     */
    @Deprecated
    @Column(name = "description")
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    private String description;

    @Column(name = "genbank_type", length = 127)
    private String genbankType;

    @Column(name = "uri")
    private String uri;

    @Column(name = "flag")
    @Enumerated(EnumType.STRING)
    private AnnotationType annotationType;

    @OneToMany(cascade = {CascadeType.ALL}, fetch = FetchType.EAGER, mappedBy = "sequenceFeature")
    private final Set<SequenceFeatureAttribute> sequenceFeatureAttributes = new LinkedHashSet<>();

    public SequenceFeature() {
        super();
    }

    public SequenceFeature(Sequence sequence, Feature feature, int strand, String name,
            String genbankType, AnnotationType annotationType) {
        super();
        this.sequence = sequence;
        this.feature = feature;
        this.strand = strand;
        this.name = name;
        this.genbankType = genbankType;
        this.annotationType = annotationType;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public DNAFeature toDataTransferObject() {
        DNAFeature dnaFeature = new DNAFeature();
        dnaFeature.setName(this.name);
        dnaFeature.setType(this.genbankType);
        return dnaFeature;
    }

    /**
     * Annotation type for "parts".
     * <p/>
     * Parts can have a PREFIX, a SUFFIX, a SCAR features. The INNER and SUBINNER features indicate
     * part sequence excluding the prefix and suffix.
     *
     * @author Timothy Ham
     */
    public enum AnnotationType {
        PREFIX, SUFFIX, SCAR, INNER, SUBINNER;
    }

    public void setId(long id) {
        this.id = id;
    }

    @XmlTransient
    public long getId() {
        return id;
    }

    @XmlTransient
    public Sequence getSequence() {
        return sequence;
    }

    public void setSequence(Sequence sequence) {
        this.sequence = sequence;
    }

    public Feature getFeature() {
        return feature;
    }

    public void setFeature(Feature feature) {
        this.feature = feature;
    }

    public Set<AnnotationLocation> getAnnotationLocations() {
        return annotationLocations;
    }

    public int getStrand() {
        return strand;
    }

    /**
     * +1 for forward, -1 for reverse.
     */
    public void setStrand(int strand) {
        this.strand = strand;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGenbankType() {
        return genbankType;
    }

    public AnnotationType getAnnotationType() {
        return annotationType;
    }

    public Set<SequenceFeatureAttribute> getSequenceFeatureAttributes() {
        return sequenceFeatureAttributes;
    }

    public Integer getUniqueGenbankStart() {
        Integer result = null;
        if (getAnnotationLocations() != null && getAnnotationLocations().size() == 1) {
            result = ((AnnotationLocation) getAnnotationLocations().toArray()[0]).getGenbankStart();
        }
        return result;
    }

    public Integer getUniqueEnd() {
        Integer result = null;
        if (getAnnotationLocations() != null && getAnnotationLocations().size() == 1) {
            result = ((AnnotationLocation) getAnnotationLocations().toArray()[getAnnotationLocations()
                    .size() - 1]).getEnd();
        }
        return result;
    }
}
