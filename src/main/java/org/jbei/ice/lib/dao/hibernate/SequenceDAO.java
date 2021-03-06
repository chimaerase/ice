package org.jbei.ice.lib.dao.hibernate;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.jbei.ice.lib.common.logging.Logger;
import org.jbei.ice.lib.dao.DAOException;
import org.jbei.ice.lib.dto.entry.Visibility;
import org.jbei.ice.lib.entry.model.Entry;
import org.jbei.ice.lib.models.AnnotationLocation;
import org.jbei.ice.lib.models.Feature;
import org.jbei.ice.lib.models.Sequence;
import org.jbei.ice.lib.models.SequenceFeature;
import org.jbei.ice.lib.utils.SequenceUtils;
import org.jbei.ice.lib.utils.UtilityException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

/**
 * Manipulate {@link Sequence} and associated objects in the database.
 *
 * @author Hector Plahar, Timothy Ham, Zinovii Dmytriv
 */
public class SequenceDAO extends HibernateRepository<Sequence> {
    /**
     * Save the given {@link Sequence} object in the database.
     *
     * @param sequence sequence to save
     * @return Saved Sequence object
     * @throws DAOException
     */
    public Sequence saveSequence(Sequence sequence) {
        if (sequence == null) {
            throw new DAOException("Failed to save null sequence!");
        }

        if (sequence.getEntry() == null) {
            throw new DAOException("Failed to save sequence without entry!");
        }

        Set<SequenceFeature> sequenceFeatureSet = null;

        normalizeAnnotationLocations(sequence);
        if (sequence.getSequenceFeatures() != null) {
            sequenceFeatureSet = new HashSet<>(sequence.getSequenceFeatures());
            sequence.setSequenceFeatures(null);
        }
        sequence = super.create(sequence);

        if (sequenceFeatureSet != null) {
            for (SequenceFeature sequenceFeature : sequenceFeatureSet) {
                Feature feature = sequenceFeature.getFeature();

                if (feature == null) {
                    throw new DAOException("SequenceFeature has no feature");
                }

                Feature existingFeature = getFeatureBySequence(feature.getSequence());

                if (existingFeature == null) { // new feature -> save it
                    existingFeature = saveFeature(feature); // tODO : this needs to be handled by another DAO
                } else {
                    if (!sameFeatureUri(existingFeature, feature)) {
                        // same sequence feature but different uri
                        // sequence hash fwa uniqueness causes problems when trying to save a new feature with same seq
                        existingFeature.setUri(feature.getUri());
                    }
                }

                sequenceFeature.setFeature(existingFeature);
                sequenceFeature.setSequence(sequence);
                currentSession().saveOrUpdate(sequenceFeature);
            }
        }

        return sequence;
    }

    private boolean sameFeatureUri(Feature f1, Feature f2) {
        if (f1.getUri() == null && f2.getUri() == null)
            return true;

        if (f1.getIdentification() != null && !f1.getIdentification().equalsIgnoreCase(f2.getIdentification()))
            return false;

        if (f1.getUri() != null && !f1.getUri().equalsIgnoreCase(f2.getUri()))
            return false;

        return f2.getUri().equalsIgnoreCase(f1.getUri());
    }

    public Sequence updateSequence(Sequence sequence, Set<SequenceFeature> newFeatures) {
        if (sequence == null) {
            throw new DAOException("Failed to update null sequence!");
        }

        if (sequence.getEntry() == null) {
            throw new DAOException("Failed to update sequence without entry!");
        }

        // clear features
        for (SequenceFeature feature : sequence.getSequenceFeatures())
            currentSession().delete(feature);
        sequence.setSequenceFeatures(null);
        sequence = update(sequence);

        // new features
        if (newFeatures != null) {
            for (SequenceFeature sequenceFeature : newFeatures) {
                Feature newFeature = sequenceFeature.getFeature();
                Feature newFeatureExisting = getFeatureBySequence(newFeature.getSequence());
                if (newFeatureExisting == null) {
                    newFeatureExisting = saveFeature(newFeature);
                }
                sequenceFeature.setFeature(newFeatureExisting);
                sequenceFeature.setSequence(sequence);
                currentSession().saveOrUpdate(sequenceFeature);
            }
        }

        return sequence;
    }

    /**
     * Delete the given {@link Sequence} object in the database.
     *
     * @param sequence          sequence to delete
     * @param pigeonImageFolder path of the image folder where the pigeon images are cached
     */
    public void deleteSequence(Sequence sequence, String pigeonImageFolder) {
        String sequenceHash = sequence.getFwdHash();
        try {
            sequence.setEntry(null);
            sequence.getSequenceFeatures();
            super.delete(sequence);
            currentSession().flush();
            Files.deleteIfExists(Paths.get(pigeonImageFolder, sequenceHash + ".png"));
        } catch (IOException e) {
            Logger.warn(e.getMessage());
        } catch (HibernateException he) {
            Logger.error(he);
            throw new DAOException(he);
        }
    }

    /**
     * Save the given {@link Feature} object in the database.
     *
     * @param feature feature to save
     * @return Saved Feature object.
     */
    public Feature saveFeature(Feature feature) {
        if (feature == null) {
            throw new DAOException("Cannot save null object");
        }

        Session session = currentSession();
        try {
            session.save(feature);
        } catch (HibernateException e) {
            Logger.error(e);
            throw new DAOException("Exception saving feature", e);
        }

        return feature;
    }

    /**
     * Retrieve the {@link Sequence} object associated with the given {@link Entry} object.
     *
     * @param entry entry associated with sequence
     * @return Sequence object.
     */
    public Sequence getByEntry(Entry entry) {
        Sequence sequence = null;

        Session session = currentSession();
        try {
            String queryString = "from " + Sequence.class.getName() + " as sequence where sequence.entry = :entry";
            Query query = session.createQuery(queryString);
            query.setEntity("entry", entry);
            Object queryResult = query.uniqueResult();

            if (queryResult != null) {
                sequence = (Sequence) queryResult;
            }
        } catch (HibernateException e) {
            Logger.error(e);
            throw new DAOException("Failed to retrieve sequence by entry: " + entry.getId(), e);
        }
        normalizeAnnotationLocations(sequence);
        return sequence;
    }

    public boolean hasSequence(long entryId) {
        Session session = currentSession();
        try {
            Number itemCount = (Number) session.createCriteria(Sequence.class)
                                               .setProjection(Projections.countDistinct("id"))
                                               .add(Restrictions.eq("entry.id", entryId)).uniqueResult();
            return itemCount.intValue() > 0;
        } catch (HibernateException e) {
            throw new DAOException("Failed to retrieve sequence by entry: " + entryId, e);
        }
    }

    public String getSequenceFilename(Entry entry) {
        return (String) currentSession().createCriteria(Sequence.class)
                .setProjection(Projections.property("fileName"))
                .add(Restrictions.eq("entry", entry))
                .uniqueResult();
    }

    /**
     * Determines if the user uploaded a sequence file and associated it with an entry
     *
     * @param entryId unique identifier for entry
     * @return true if there is a sequence file that was originally uploaded by user, false otherwise
     * @throws DAOException
     */
    public boolean hasOriginalSequence(long entryId) {
        Session session = currentSession();
        try {
            Number itemCount = (Number) session.createCriteria(Sequence.class)
                                               .setProjection(Projections.countDistinct("id"))
                                               .add(Restrictions.eq("entry.id", entryId))
                                               .add(Restrictions.conjunction().add(
                                                       Restrictions.ne("sequenceUser", "")).add(
                                                       Restrictions.isNotNull("sequenceUser")))
                                               .uniqueResult();

            return itemCount.intValue() > 0;
        } catch (HibernateException e) {
            throw new DAOException("Failed to retrieve sequence by entry: " + entryId, e);
        }
    }

    /**
     * Enables retrieving sequences in the database without loading everything memory
     * <p>
     * Expected usage is
     * <code>
     *     long count = getSequenceCount();
     *     int offset = 0;
     *     while( offset < count ) {
     *         Sequence sequence = dao.getSequence(offset);
     *         // do something with sequence
     *     }
     * </code>
     *
     * @return Sequence at the specified offset
     * @throws DAOException
     */
    @SuppressWarnings("unchecked")
    public Sequence getSequence(int offset) throws DAOException {
        Session session = currentSession();
        try {
            Criteria criteria = session.createCriteria(Sequence.class);

            Criteria entryC = criteria.createCriteria("entry", "entry");
            entryC.add(Restrictions.disjunction()
                    .add(Restrictions.eq("visibility", Visibility.OK.getValue())));
            criteria.setFirstResult(offset);
            criteria.setMaxResults(1);
            return (Sequence) criteria.uniqueResult();
        } catch (HibernateException he) {
            Logger.error(he);
            throw new DAOException(he);
        }
    }

    /**
     * @return number of sequences available for all valid (visibility=9) entry object
     */
    public int getSequenceCount() {
        Session session = currentSession();
        try {
            Criteria criteria = session.createCriteria(Sequence.class);

            Criteria entryC = criteria.createCriteria("entry", "entry");
            entryC.add(Restrictions.disjunction()
                    .add(Restrictions.eq("visibility", Visibility.OK.getValue())));
            criteria.setProjection(Projections.count("id"));
            Number number = (Number) criteria.uniqueResult();
            return number.intValue();
        } catch (HibernateException he) {
            Logger.error(he);
            throw new DAOException(he);
        }
    }

    /**
     * Retrieve the {@link Feature} object with the given DNA sequence string.
     *
     * @param featureDnaSequence dna sequence of feature
     * @return Feature object.
     * @throws DAOException
     */
    private Feature getFeatureBySequence(String featureDnaSequence) {
        featureDnaSequence = featureDnaSequence.toLowerCase();
        Feature result;
        Session session = currentSession();

        try {
            String queryString = "from " + Feature.class.getName() + " where hash = :hash";
            Query query = session.createQuery(queryString);
            query.setParameter("hash", SequenceUtils.calculateSequenceHash(featureDnaSequence));

            Object queryResult = query.uniqueResult();

            if (queryResult != null) {
                result = (Feature) queryResult;
            } else {
                String reverseComplement = SequenceUtils.reverseComplement(featureDnaSequence);
                String sequenceHash = SequenceUtils.calculateSequenceHash(reverseComplement);
                query.setParameter("hash", sequenceHash);
                queryResult = query.uniqueResult();
                if (queryResult != null) {
                    result = (Feature) queryResult;
                } else {
                    result = null;
                }
            }
        } catch (HibernateException | UtilityException e) {
            Logger.error(e);
            throw new DAOException("Failed to get Feature by sequence!", e);
        }

        return result;
    }

    /**
     * Normalize {@link AnnotationLocation}s by fixing strangely defined annotationLocations.
     * <p/>
     * Fix locations that encompass the entire sequence, but defined strangely. This causes problems
     * elsewhere.
     *
     * @param sequence
     * @return
     */
    private static Sequence normalizeAnnotationLocations(Sequence sequence) {
        if (sequence == null) {
            return null;
        }
        int length = sequence.getSequence().length();
        boolean wholeSequence;
        for (SequenceFeature sequenceFeature : sequence.getSequenceFeatures()) {
            wholeSequence = false;
            Set<AnnotationLocation> locations = sequenceFeature.getAnnotationLocations();
            for (AnnotationLocation location : locations) {
                if (location.getGenbankStart() == location.getEnd() + 1) {
                    wholeSequence = true;
                }
            }
            if (wholeSequence) {
                sequenceFeature.setStrand(1);
                sequenceFeature.getAnnotationLocations().clear();
                sequenceFeature.getAnnotationLocations().add(
                        new AnnotationLocation(1, length, sequenceFeature));
            }
        }
        return sequence;
    }

    @Override
    public Sequence get(long id) {
        return super.get(Sequence.class, id);
    }
}
