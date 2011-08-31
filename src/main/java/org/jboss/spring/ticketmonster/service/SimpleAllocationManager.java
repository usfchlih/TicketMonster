package org.jboss.spring.ticketmonster.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.jboss.spring.ticketmonster.domain.Allocation;
import org.jboss.spring.ticketmonster.domain.BookingState;
import org.jboss.spring.ticketmonster.domain.CacheKey;
import org.jboss.spring.ticketmonster.domain.RowReservation;
import org.jboss.spring.ticketmonster.domain.SeatBlock;
import org.jboss.spring.ticketmonster.domain.SectionRow;
import org.jboss.spring.ticketmonster.domain.Show;
import org.jboss.spring.ticketmonster.repo.ShowDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;

/**
 * Implementation of the AllocationManager interface.
 * 
 * @author Ryan Bradley
 *
 */

public class SimpleAllocationManager implements AllocationManager {

	@Autowired
	private ShowDao showDao;
	
	@Autowired
	private CacheManager cacheManager;
	
	@Autowired
	private BookingState bookingState;
	
	private static final boolean PURCHASED = true;
	
	public List<Allocation> finalizeReservations(List<SeatBlock> reservations) {
		List<Allocation> allocations = new ArrayList<Allocation>();
		
		for(SeatBlock block : reservations) {
			Allocation allocation = this.createAllocation(block);
			allocations.add(allocation);
			persistChanges(block);
		}		
		
		return allocations;
	}

	public Allocation createAllocation(SeatBlock block) {
		Allocation allocation = new Allocation();
		allocation.setAssigned(new Date());
		allocation.setStartSeat(block.getStartSeat());
		allocation.setEndSeat(block.getEndSeat());
		
		SectionRow row = showDao.findSectionRow(block.getKey().getRowId());
		allocation.setRow(row);
		
		Show show = showDao.getShow(block.getKey().getShowId());
		allocation.setShow(show);
		
		allocation.setQuantity(block.getEndSeat()-block.getStartSeat()+1);
		allocation.setUser(bookingState.getUser());
		
		return allocation;
	}
	
	public void persistChanges(SeatBlock block) {
		ConcurrentMapCache reservationsCache = (ConcurrentMapCache) cacheManager.getCache("reservations");
		CacheKey key = block.getKey();
		
		RowReservation reservation = (RowReservation) reservationsCache.get(key).get();
		LinkedList<SeatBlock> reservedSeats = reservation.getReservedSeats();
		
		for(SeatBlock reserved : reservedSeats) {
			if(reserved.equals(block)) {
				reserved.setPurchased(PURCHASED);
				continue;
			}
		}
		
		reservation.setReservedSeats(reservedSeats);
		reservationsCache.put(key, reservation);
	}
	
}