package com.schemebridge.coreservice.citizen.repository;

import com.schemebridge.coreservice.citizen.model.CitizenDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CitizenRepository extends MongoRepository<CitizenDocument, String> {

    Optional<CitizenDocument> findByAuthUserIdAndDeletedFalse(String authUserId);

    boolean existsByAuthUserIdAndDeletedFalse(String authUserId);

    @Query("{ 'deleted': false, " +
           "  '$and': [ " +
           "    { '$or': [ { '?0': null }, { 'addressDetails.state': '?0' } ] }, " +
           "    { '$or': [ { '?1': null }, { 'addressDetails.district': '?1' } ] }, " +
           "    { '$or': [ { '?2': null }, { 'personalDetails.category': '?2' } ] }, " +
           "    { '$or': [ { '?3': null }, { 'occupationDetails.occupationType': '?3' } ] }, " +
           "    { '$or': [ { '?4': null }, { 'specialCategoryDetails.isFarmer': ?4 } ] }, " +
           "    { '$or': [ { '?5': null }, { 'specialCategoryDetails.isMinority': ?5 } ] }, " +
           "    { '$or': [ { '?6': null }, { 'specialCategoryDetails.isDisabled': ?6 } ] } " +
           "  ] }")
    List<CitizenDocument> searchCitizens(String state, String district, String category, String occupationType,
                                         Boolean farmer, Boolean minority, Boolean disability);
}
