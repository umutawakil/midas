package com.midas.repositories

import com.midas.domain.Milestone
import org.springframework.data.repository.CrudRepository

interface MilestoneRepository: CrudRepository<Milestone, Long> {
}