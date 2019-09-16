function rs = squareRoot(x)
if x < 0
    error('SQUAREROOT:INVALIDINPUT','Negative value %d Not accepted',x);
else
    rs = sqrt(x);
end
end