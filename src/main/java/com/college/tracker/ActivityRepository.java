package com.college.tracker;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
    
    // Search Method 1: Select Type AND search by Keyword
    @Query("SELECT a FROM Activity a WHERE a.activityType = :type AND " +
           "(LOWER(a.facultyName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Activity> searchMethodOne(@Param("type") String type, @Param("keyword") String keyword);

    // Search Method 2 (All types - no filter): Search by Faculty Name, Year, and NITTT
    @Query("SELECT a FROM Activity a WHERE " +
           "LOWER(a.facultyName) LIKE LOWER(CONCAT('%', :faculty, '%')) AND " +
           "a.date LIKE CONCAT('%', :year, '%') AND " +
           "(:nittt IS NULL OR a.nitttCertified = :nittt)")
    List<Activity> searchMethodTwo(@Param("faculty") String faculty,
                                   @Param("year") String year,
                                   @Param("nittt") Boolean nittt);

    // Search Method 2 with multi-category type filter (IN clause)
    @Query("SELECT a FROM Activity a WHERE " +
           "a.activityType IN :types AND " +
           "LOWER(a.facultyName) LIKE LOWER(CONCAT('%', :faculty, '%')) AND " +
           "a.date LIKE CONCAT('%', :year, '%') AND " +
           "(:nittt IS NULL OR a.nitttCertified = :nittt)")
    List<Activity> searchMethodTwoFiltered(@Param("types") List<String> types,
                                           @Param("faculty") String faculty,
                                           @Param("year") String year,
                                           @Param("nittt") Boolean nittt);
}