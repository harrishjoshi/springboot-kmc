#!/bin/bash
# Script to diagnose and fix Docker DNS issues
# Run with: sudo bash fix-docker-dns.sh

echo "=== Docker DNS Issue Diagnostics & Fix ==="
echo ""

# Check 1: Current iptables rules
echo "1. Checking iptables rules for DNS blocking..."
iptables -L OUTPUT -n -v | grep "dpt:53"
if [ $? -eq 0 ]; then
    echo "   ⚠️  Found DNS blocking rules in iptables"
else
    echo "   ✓ No explicit DNS blocking rules in OUTPUT chain"
fi
echo ""

# Check 2: Docker daemon configuration
echo "2. Checking Docker daemon configuration..."
cat /etc/docker/daemon.json 2>/dev/null || echo "   No daemon.json file found"
echo ""

# Check 3: AppArmor status
echo "3. Checking AppArmor status..."
if command -v aa-status &> /dev/null; then
    aa-status --enabled && echo "   AppArmor is enabled" || echo "   AppArmor is disabled"
else
    echo "   AppArmor not installed"
fi
echo ""

# Check 4: SELinux status
echo "4. Checking SELinux status..."
if command -v getenforce &> /dev/null; then
    getenforce
else
    echo "   SELinux not installed"
fi
echo ""

# Check 5: UFW (Uncomplicated Firewall) status
echo "5. Checking UFW firewall..."
if command -v ufw &> /dev/null; then
    ufw status
else
    echo "   UFW not installed"
fi
echo ""

echo "=== Attempting Fixes ==="
echo ""

# Fix 1: Ensure Docker daemon uses correct DNS
echo "Fix 1: Updating Docker daemon.json with DNS servers..."
cat > /etc/docker/daemon.json <<EOF
{
  "dns": ["8.8.8.8", "1.1.1.1"],
  "dns-search": ["."],
  "dns-opts": ["ndots:0"]
}
EOF
echo "   ✓ Updated /etc/docker/daemon.json"
echo ""

# Fix 2: Allow DNS through iptables for Docker
echo "Fix 2: Adding iptables rules to allow DNS for Docker..."
# Allow DNS for docker0 bridge
iptables -I OUTPUT -o docker0 -p udp --dport 53 -j ACCEPT
iptables -I OUTPUT -o docker0 -p tcp --dport 53 -j ACCEPT
# Allow DNS for containers
iptables -I DOCKER-USER -p udp --dport 53 -j ACCEPT 2>/dev/null || echo "   DOCKER-USER chain not found, skipping"
iptables -I DOCKER-USER -p tcp --dport 53 -j ACCEPT 2>/dev/null || echo "   DOCKER-USER chain not found, skipping"
echo "   ✓ Added iptables rules for DNS"
echo ""

# Fix 3: Make iptables rules persistent
echo "Fix 3: Making iptables rules persistent..."
if command -v iptables-save &> /dev/null; then
    iptables-save > /etc/iptables/rules.v4 2>/dev/null || \
    iptables-save > /etc/iptables.rules 2>/dev/null || \
    echo "   ⚠️  Could not save iptables rules (iptables-persistent not installed)"
    echo "   Run: apt-get install iptables-persistent"
fi
echo ""

# Fix 4: Restart Docker daemon
echo "Fix 4: Restarting Docker daemon..."
systemctl restart docker
sleep 3
systemctl status docker --no-pager | head -5
echo ""

# Fix 5: Test DNS resolution from container
echo "Fix 5: Testing DNS resolution from container..."
docker run --rm alpine nslookup repo.maven.apache.org
if [ $? -eq 0 ]; then
    echo "   ✓ DNS resolution working!"
else
    echo "   ✗ DNS resolution still failing"
    echo ""
    echo "   Additional steps needed:"
    echo "   1. Check if systemd-resolved is interfering:"
    echo "      systemctl status systemd-resolved"
    echo "   2. Try disabling IPv6 for Docker:"
    echo "      Add to /etc/docker/daemon.json: \"ipv6\": false"
    echo "   3. Check UFW/firewall logs:"
    echo "      journalctl -u docker -n 50"
fi
echo ""

echo "=== Done ==="
echo "If DNS is now working, run: ./build.sh"
