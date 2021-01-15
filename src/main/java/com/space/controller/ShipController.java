package com.space.controller;

import com.space.model.Ship;
import com.space.model.ShipType;
import com.space.service.ShipServiceImpl;
import com.space.service.ShipSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping(value = "/rest")
public class ShipController {

    private final ShipServiceImpl shipServiceImpl;
    private final ShipSpecification shipSpecification;

    @Autowired
    public ShipController(ShipServiceImpl shipServiceImpl, ShipSpecification specification) {
        this.shipServiceImpl = shipServiceImpl;
        this.shipSpecification = specification;
    }

    @RequestMapping(value = "/ships", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public ResponseEntity<List<Ship>> getShipsList(@RequestParam(required = false) String name,
                                                   @RequestParam(required = false) String planet,
                                                   @RequestParam(required = false) ShipType shipType,
                                                   @RequestParam(required = false) Long after,
                                                   @RequestParam(required = false) Long before,
                                                   @RequestParam(required = false) Boolean isUsed,
                                                   @RequestParam(required = false) Double minSpeed,
                                                   @RequestParam(required = false) Double maxSpeed,
                                                   @RequestParam(required = false) Integer minCrewSize,
                                                   @RequestParam(required = false) Integer maxCrewSize,
                                                   @RequestParam(required = false) Double minRating,
                                                   @RequestParam(required = false) Double maxRating,
                                                   @RequestParam(required = false, defaultValue = "ID") ShipOrder order,
                                                   @RequestParam(required = false, defaultValue = "0") Integer pageNumber,
                                                   @RequestParam(required = false, defaultValue = "3") Integer pageSize
    ) {

        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(order.getFieldName()));

        Specification<Ship> specification = Specification.where(shipSpecification.getByName(name)
                .and(shipSpecification.getByPlanet(planet))
                .and(shipSpecification.getByShipType(shipType))
                .and(shipSpecification.getByProdDate(after, before))
                .and(shipSpecification.getByUsed(isUsed))
                .and(shipSpecification.getBySpeed(minSpeed, maxSpeed))
                .and(shipSpecification.getByCrewSize(minCrewSize, maxCrewSize))
                .and(shipSpecification.getByRating(minRating, maxRating)));

        List<Ship> allShips = this.shipServiceImpl.getAll(specification, pageable).getContent();
        if (allShips.size() == 0) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(allShips, HttpStatus.OK);
    }

    @RequestMapping(value = "/ships/count", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Integer> getShipsCount(@RequestParam(required = false) String name,
                                                 @RequestParam(required = false) String planet,
                                                 @RequestParam(required = false) ShipType shipType,
                                                 @RequestParam(required = false) Long after,
                                                 @RequestParam(required = false) Long before,
                                                 @RequestParam(required = false) Boolean isUsed,
                                                 @RequestParam(required = false) Double minSpeed,
                                                 @RequestParam(required = false) Double maxSpeed,
                                                 @RequestParam(required = false) Integer minCrewSize,
                                                 @RequestParam(required = false) Integer maxCrewSize,
                                                 @RequestParam(required = false) Double minRating,
                                                 @RequestParam(required = false) Double maxRating) {


        Specification<Ship> specification = Specification.where(shipSpecification.getByName(name)
                .and(shipSpecification.getByPlanet(planet))
                .and(shipSpecification.getByShipType(shipType))
                .and(shipSpecification.getByProdDate(after, before))
                .and(shipSpecification.getByUsed(isUsed))
                .and(shipSpecification.getBySpeed(minSpeed, maxSpeed))
                .and(shipSpecification.getByCrewSize(minCrewSize, maxCrewSize))
                .and(shipSpecification.getByRating(minRating, maxRating)));

        return new ResponseEntity<>(this.shipServiceImpl.getCount(specification), HttpStatus.OK);
    }

    @RequestMapping(value = "/ships", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public ResponseEntity<Ship> createShip(@RequestBody Ship ship) {
        if (ship.getName() == null
                || ship.getPlanet() == null
                || ship.getShipType() == null
                || ship.getProdDate() == null
                || ship.getSpeed() == null
                || ship.getCrewSize() == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        if (ship.getName().length() > 50
                || ship.getName().length() == 0
                || ship.getPlanet().length() > 50
                || ship.getPlanet().length() == 0
                || ship.getSpeed() < 0.01
                || ship.getSpeed() > 0.99
                || ship.getCrewSize() < 1
                || ship.getCrewSize() > 9999
                || getProdYear(ship.getProdDate()) < 2800
                || getProdYear(ship.getProdDate()) > 3019
        ) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        if (ship.getUsed() == null) {
            ship.setUsed(false);
        }
        Double rating = countRating(ship);
        ship.setRating(rating);
        shipServiceImpl.save(ship);
        return new ResponseEntity<>(ship, HttpStatus.OK);
    }

    @RequestMapping(value = "/ships/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public ResponseEntity<Ship> getShip(@PathVariable("id") Long id) {
        if (id == null || id <= 0) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        Ship ship = this.shipServiceImpl.getById(id);
        if (ship == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(ship, HttpStatus.OK);
    }

    @RequestMapping(value = "/ships/{id}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public ResponseEntity<Ship> updateShip(@PathVariable("id") Long id, @RequestBody Ship ship) {
        if (id == null || id <= 0) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        Ship shipFromDB = this.shipServiceImpl.getById(id);
        if (shipFromDB == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        if (ship.getName() != null) {
            if (ship.getName().length() <= 50 && !(ship.getName().length() == 0)) {
                shipFromDB.setName(ship.getName());
            } else return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        if (ship.getPlanet() != null) {
            if (ship.getPlanet().length() <= 50 && !(ship.getPlanet().length() == 0)) {
                shipFromDB.setPlanet(ship.getPlanet());
            } else return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        if (ship.getShipType() != null) {
            shipFromDB.setShipType(ship.getShipType());
        }
        if (ship.getProdDate() != null) {
            if (getProdYear(ship.getProdDate()) > 2800 && getProdYear(ship.getProdDate()) < 3019) {
                shipFromDB.setProdDate(ship.getProdDate());
            } else return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        }
        if (ship.getUsed() != null) {
            shipFromDB.setUsed(ship.getUsed());
        }
        if (ship.getSpeed() != null) {
            if (ship.getSpeed() >= 0.01 && ship.getSpeed() <= 0.99) {
                shipFromDB.setSpeed(ship.getSpeed());
            } else return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        if (ship.getCrewSize() != null) {
            if (ship.getCrewSize() >= 1 && ship.getCrewSize() <= 9999) {
                shipFromDB.setCrewSize(ship.getCrewSize());
            } else return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        Double rating = countRating(shipFromDB);
        shipFromDB.setRating(rating);

        this.shipServiceImpl.save(shipFromDB);
        return new ResponseEntity<>(shipFromDB, HttpStatus.OK);
    }

    @RequestMapping(value = "/ships/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Ship> deleteShip(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        Ship ship = this.shipServiceImpl.getById(id);
        if (ship == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        this.shipServiceImpl.delete(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private Integer getProdYear(Date prodDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(prodDate);
        return calendar.get(Calendar.YEAR);

    }

    private Double countRating(Ship ship) {
        Double k = ship.getUsed() ? 0.5 : 1;
        Integer currentYear = 3019;
        Integer prodYear = getProdYear(ship.getProdDate());
        BigDecimal rating = BigDecimal.valueOf((80 * ship.getSpeed() * k) /
                (currentYear - prodYear + 1)).setScale(2, RoundingMode.HALF_UP);
        return rating.doubleValue();
    }
}