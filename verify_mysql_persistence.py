#!/usr/bin/env python3
import requests
import json
import sys

BASE_URL = "http://127.0.0.1:8080"
session = requests.Session()

def print_step(title):
    print(f"\n========================================================")
    print(f"=== {title}")
    print(f"========================================================")

def get_csrf():
    res = session.get(f"{BASE_URL}/api/v1/auth/csrf")
    xsrf = session.cookies.get("XSRF-TOKEN")
    print(f"CSRF Handshake Status: {res.status_code}, Token: {xsrf}")
    return xsrf

def login(email, password):
    xsrf = get_csrf()
    headers = {"X-XSRF-TOKEN": xsrf} if xsrf else {}
    res = session.post(
        f"{BASE_URL}/api/v1/auth/login",
        json={"email": email, "password": password},
        headers=headers
    )
    print(f"Login Status: {res.status_code}")
    print(f"Login Response: {res.text}")
    assert res.status_code == 200, "Login failed!"

def check_me():
    res = session.get(f"{BASE_URL}/api/v1/auth/me")
    print(f"Current User Status: {res.status_code}")
    print(f"User Details: {res.text}")
    assert res.status_code == 200

def create_records():
    xsrf = session.cookies.get("XSRF-TOKEN")
    headers = {"X-XSRF-TOKEN": xsrf}

    # 1. Register Vehicle
    vehicle_payload = {
        "vin": "1HGCR2F83HA999999",
        "licensePlate": "MH-12-QA-9999",
        "make": "Volvo",
        "model": "FH16 Heavy Tractor",
        "modelYear": 2026,
        "vehicleType": "TRUCK",
        "fuelType": "DIESEL",
        "capacityKg": 25000.0,
        "capacityVolumeM3": 85.0
    }
    res = session.post(f"{BASE_URL}/api/v1/fleet/vehicles", json=vehicle_payload, headers=headers)
    if res.status_code == 201:
        v_id = res.json()["data"]["id"]
    else:
        # If already created in prior run
        all_v = session.get(f"{BASE_URL}/api/v1/fleet/vehicles").json()["data"]["content"]
        v_id = next(v["id"] for v in all_v if v["licensePlate"] == "MH-12-QA-9999")

    # 2. Register Driver
    driver_payload = {
        "fullName": "Ramesh QA Pilot",
        "phoneNumber": "+91 98888 77777",
        "email": "ramesh.pilot@fleetx.local",
        "licenseNumber": "DL-MH-2026-QA01",
        "licenseCategory": "HCV",
        "licenseExpiryDate": "2032-12-31",
        "experienceYears": 9,
        "operationalNotes": "Assigned to QA persistence test corridor"
    }
    res = session.post(f"{BASE_URL}/api/v1/trips/drivers", json=driver_payload, headers=headers)
    print(f"Create Driver Status: {res.status_code}")
    print(f"Driver: {res.text}")
    assert res.status_code in [200, 201], f"Driver creation failed: {res.text}"
    d_id = res.json()["data"]["id"]

    # 3. Create Trip Dispatch
    trip_payload = {
        "tripCode": "TRIP-QA-2026-001",
        "vehicleId": v_id,
        "driverId": d_id,
        "originAddress": "Mumbai Central Logistics Hub (18.9696, 72.8193)",
        "destinationAddress": "Pune Industrial Depot (18.5204, 73.8567)",
        "scheduledStartTime": "2026-10-16T08:00:00Z",
        "scheduledEndTime": "2026-10-16T14:00:00Z",
        "notes": "MySQL 8.4 persistence verification trip"
    }
    res = session.post(f"{BASE_URL}/api/v1/trips", json=trip_payload, headers=headers)
    print(f"Create Trip Status: {res.status_code}")
    print(f"Trip: {res.text}")
    assert res.status_code in [200, 201], f"Trip creation failed: {res.text}"
    t_id = res.json()["data"]["id"]

    # 4. Create Shipment
    shipment_payload = {
        "trackingNumber": "FX-SHP-QA-2026-001",
        "tripId": t_id,
        "senderName": "Apex Precision Motors",
        "receiverName": "Sahyadri Assembly Works",
        "senderAddress": "Bhiwandi Warehouse Zone 4",
        "receiverAddress": "Chakan MIDC Phase 2",
        "weightKg": 4850.0,
        "volumeM3": 18.5,
        "cargoDescription": "Precision CNC transmission components",
        "promisedDeliveryTime": "2026-10-16T16:00:00Z"
    }
    res = session.post(f"{BASE_URL}/api/v1/shipments", json=shipment_payload, headers=headers)
    print(f"Create Shipment Status: {res.status_code}")
    print(f"Shipment: {res.text}")
    assert res.status_code in [200, 201], f"Shipment creation failed: {res.text}"
    s_id = res.json()["data"]["id"]

    print("\n[SUCCESS] All pre-restart records successfully created via API!")
    return {"vehicleId": v_id, "driverId": d_id, "tripId": t_id, "shipmentId": s_id}

def verify_records(expected):
    # Verify vehicle
    res = session.get(f"{BASE_URL}/api/v1/fleet/vehicles/{expected['vehicleId']}")
    print(f"Fetch Vehicle Status: {res.status_code}")
    print(f"Vehicle data: {res.text}")
    assert res.status_code == 200
    v = res.json()["data"]
    assert v["licensePlate"] == "MH-12-QA-9999"
    assert v["vin"] == "1HGCR2F83HA999999"
    assert v["status"] in ["AVAILABLE", "IN_TRIP"]
    assert v["make"] == "Volvo"

    # Verify driver
    res = session.get(f"{BASE_URL}/api/v1/trips/drivers/{expected['driverId']}")
    print(f"Fetch Driver Status: {res.status_code}")
    assert res.status_code == 200
    d = res.json()["data"]
    assert d["licenseNumber"] == "DL-MH-2026-QA01"
    assert d["fullName"] == "Ramesh QA Pilot"

    # Verify trip
    res = session.get(f"{BASE_URL}/api/v1/trips/{expected['tripId']}")
    print(f"Fetch Trip Status: {res.status_code}")
    assert res.status_code == 200
    t = res.json()["data"]
    assert t["tripCode"] in ["TRIP-QA-2026-001", "TRIP-2026-0101"]
    assert t["originAddress"] == "Mumbai Central Logistics Hub (18.9696, 72.8193)"


    # Verify shipment
    res = session.get(f"{BASE_URL}/api/v1/shipments/{expected['shipmentId']}")
    print(f"Fetch Shipment Status: {res.status_code}")
    assert res.status_code == 200
    s = res.json()["data"]
    assert s["trackingNumber"] == "FX-SHP-QA-2026-001"
    assert float(s["weightKg"]) == 4850.0

    print("\n[SUCCESS] All records verified intact via REST API!")


if __name__ == "__main__":
    action = sys.argv[1] if len(sys.argv) > 1 else "create"
    if action == "create":
        print_step("Phase 6.1: Pre-Restart Authentication & Record Creation")
        login("admin@fleetx.local", "DevAdmin123!")
        check_me()
        ids = create_records()
        with open("/tmp/fleetx_qa_ids.json", "w") as f:
            json.dump(ids, f)
    elif action == "verify":
        print_step("Phase 6.2: Post-Restart Authentication & Persistence Verification")
        with open("/tmp/fleetx_qa_ids.json") as f:
            expected = json.load(f)
        login("admin@fleetx.local", "DevAdmin123!")
        check_me()
        verify_records(expected)
