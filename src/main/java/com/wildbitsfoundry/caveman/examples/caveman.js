function CaveArray2D(rows, cols) {
    if(rows <= 0 || cols <=0) {
        throw "the number of rows and columns have to be greater than zero"
    }
    this._rows = rows;
    this._cols = cols;
    this._data = new Array(rows * cols);
}

CaveArray2D.prototype = {
    get length() {
        return this._rows * this._cols;
    }
}

CaveArray2D.prototype.fill = function(val) {
    for(var i = 0; i < this.length; ++i) {
        this._data[i] = val;
    }
}

CaveArray2D.prototype.get = function(i, j) {
    if(i <= 0 || j <=0) {
        throw "the number of rows and columns have to be greater than zero"
    }
    return this._data[this._cols * i + j];
}

CaveArray2D.prototype.set = function(i, j, val) {
    this._data[this._cols * i + j] = val;
}


function toCaveStream(caveArray2D, variables) {
    var obj = {
        "inputdata" : {
            "rows" : caveArray2D._rows,
            "columns" : caveArray2D._cols,
        },
        "outputdata" : {
            "rows" : caveArray2D._rows,
            "columns" : caveArray2D._cols,
        },
        "variables" : variables,
        "data" : caveArray2D._data
    };
    return JSON.stringify(obj);
}